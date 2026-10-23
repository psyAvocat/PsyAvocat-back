package com.psyavocat.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

/**
 * Allège les images avant leur envoi vers Cloudflare R2 (bibliothèque standard Java
 * uniquement) : elles se chargent ensuite plus vite dans l'application mobile et sur le web.
 *
 * <ul>
 *   <li>redimensionnement progressif : côté le plus long limité selon l'usage ({@link #tailleMax(String)}) ;</li>
 *   <li>ré-encodage JPEG (qualité {@value #QUALITE_JPEG}) ; un PNG transparent reste en PNG ;</li>
 *   <li>orientation EXIF appliquée (photos de téléphone) et métadonnées retirées (position GPS…).</li>
 * </ul>
 * Rien n'est forcé : format illisible (WEBP, JPEG CMJN…), résultat pas plus léger ou erreur
 * quelconque (y compris un manque de mémoire) → l'image d'origine est envoyée telle quelle.
 */
public final class ImageOptimizer {

    private static final Logger log = LoggerFactory.getLogger(ImageOptimizer.class);

    static final float QUALITE_JPEG = 0.82f;
    static final int TAILLE_MAX_AVATAR = 800;
    static final int TAILLE_MAX_DEFAUT = 1600;
    /** Sous ce poids, une image déjà aux bonnes dimensions est envoyée telle quelle. */
    static final int POIDS_DEJA_LEGER = 150 * 1024;
    /** Une image aux bonnes dimensions n'est ré-encodée que si elle s'allège d'au moins 20 %. */
    static final double GAIN_MINIMUM = 0.8;

    /** Image prête à l'envoi : contenu, type MIME et extension de fichier. */
    public record Resultat(byte[] octets, String contentType, String extension) {
    }

    private ImageOptimizer() {
    }

    /** Photos de profil : affichées en petit. Autres médias (couvertures) : pleine largeur. */
    public static int tailleMax(String dossier) {
        return "avatars".equalsIgnoreCase(dossier) ? TAILLE_MAX_AVATAR : TAILLE_MAX_DEFAUT;
    }

    public static Resultat optimiser(byte[] original, String contentType, String extension, int tailleMax) {
        Resultat inchange = new Resultat(original, contentType, extension);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        boolean jpeg = type.equals("image/jpeg") || type.equals("image/jpg");
        boolean png = type.equals("image/png");
        if (!jpeg && !png) {
            return inchange;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return inchange;
            }
            boolean transparent = png && source.getColorModel().hasAlpha();
            boolean tropGrande = Math.max(source.getWidth(), source.getHeight()) > tailleMax;
            if (!tropGrande && original.length <= POIDS_DEJA_LEGER) {
                return inchange; // déjà petite et légère : aucun ré-encodage (pas de perte de qualité)
            }

            BufferedImage image = orienter(source, jpeg ? orientationExif(original) : 1);
            if (tropGrande) {
                image = reduire(image, tailleMax, transparent);
            }
            byte[] optimise = transparent ? encoderPng(image) : encoderJpeg(aplatir(image));

            if (!tropGrande && optimise.length > original.length * GAIN_MINIMUM) {
                return inchange; // gain trop faible pour justifier un ré-encodage
            }
            log.info("Image optimisée avant envoi : {} Ko -> {} Ko ({}x{}, max {} px)",
                    original.length / 1024, optimise.length / 1024, source.getWidth(), source.getHeight(), tailleMax);
            return transparent
                    ? new Resultat(optimise, "image/png", "png")
                    : new Resultat(optimise, "image/jpeg", "jpg");
        } catch (Throwable e) {
            // Throwable : un OutOfMemoryError ou une classe absente ne doit jamais bloquer l'envoi.
            log.warn("Optimisation d'image impossible, envoi de l'original : {}", e.toString());
            return inchange;
        }
    }

    // ------------------------------------------------------------------
    // Redimensionnement
    // ------------------------------------------------------------------

    /**
     * Réduction par moitiés successives puis passe finale bicubique : plus net qu'une
     * réduction directe, qui « saute » des pixels sur les grands écarts de taille.
     */
    static BufferedImage reduire(BufferedImage image, int tailleMax, boolean transparent) {
        double ratio = (double) tailleMax / Math.max(image.getWidth(), image.getHeight());
        int largeurCible = Math.max(1, (int) Math.round(image.getWidth() * ratio));
        int hauteurCible = Math.max(1, (int) Math.round(image.getHeight() * ratio));
        int type = transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        BufferedImage courante = image;
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        while (largeur / 2 >= largeurCible && hauteur / 2 >= hauteurCible) {
            largeur /= 2;
            hauteur /= 2;
            courante = redessiner(courante, largeur, hauteur, type, RenderingHints.VALUE_INTERPOLATION_BILINEAR, transparent);
        }
        if (largeur != largeurCible || hauteur != hauteurCible) {
            courante = redessiner(courante, largeurCible, hauteurCible, type, RenderingHints.VALUE_INTERPOLATION_BICUBIC, transparent);
        }
        return courante;
    }

    private static BufferedImage redessiner(BufferedImage source, int largeur, int hauteur, int type,
                                            Object interpolation, boolean transparent) {
        BufferedImage cible = new BufferedImage(largeur, hauteur, type);
        Graphics2D g = cible.createGraphics();
        try {
            if (!transparent) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, largeur, hauteur);
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, largeur, hauteur, null);
        } finally {
            g.dispose();
        }
        return cible;
    }

    /** JPEG sans canal alpha : les zones transparentes deviennent blanches (pas noires). */
    private static BufferedImage aplatir(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        return redessiner(image, image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
    }

    // ------------------------------------------------------------------
    // Encodage (sans métadonnées)
    // ------------------------------------------------------------------

    private static byte[] encoderJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Aucun encodeur JPEG disponible");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try (ImageOutputStream flux = ImageIO.createImageOutputStream(sortie)) {
            writer.setOutput(flux);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITE_JPEG);
            param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT); // affichage progressif
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return sortie.toByteArray();
    }

    private static byte[] encoderPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", sortie)) {
            throw new IOException("Aucun encodeur PNG disponible");
        }
        return sortie.toByteArray();
    }

    // ------------------------------------------------------------------
    // Orientation EXIF (photos prises au téléphone)
    // ------------------------------------------------------------------

    /** Orientation EXIF d'un JPEG (1 à 8), 1 si absente ou illisible. */
    static int orientationExif(byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int i = 2;
        while (i + 4 <= jpeg.length && (jpeg[i] & 0xFF) == 0xFF) {
            int marqueur = jpeg[i + 1] & 0xFF;
            if (marqueur == 0xDA || marqueur == 0xD9) {
                return 1; // début des données image : pas d'EXIF avant
            }
            int longueur = lire16(jpeg, i + 2, false);
            boolean exif = marqueur == 0xE1 && i + 10 <= jpeg.length
                    && "Exif".equals(new String(jpeg, i + 4, 4, StandardCharsets.ISO_8859_1));
            if (exif) {
                return orientationTiff(jpeg, i + 10, Math.min(jpeg.length, i + 2 + longueur));
            }
            i += 2 + longueur;
        }
        return 1;
    }

    private static int orientationTiff(byte[] d, int debut, int fin) {
        if (debut + 8 > fin) {
            return 1;
        }
        boolean petitBoutiste = d[debut] == 'I';
        int ifd = debut + lire32(d, debut + 4, petitBoutiste);
        if (ifd + 2 > fin) {
            return 1;
        }
        int entrees = lire16(d, ifd, petitBoutiste);
        for (int k = 0; k < entrees; k++) {
            int entree = ifd + 2 + k * 12;
            if (entree + 12 > fin) {
                break;
            }
            if (lire16(d, entree, petitBoutiste) == 0x0112) {
                int valeur = lire16(d, entree + 8, petitBoutiste);
                return valeur >= 1 && valeur <= 8 ? valeur : 1;
            }
        }
        return 1;
    }

    /** Remet l'image à l'endroit selon l'orientation EXIF (rotations et miroirs). */
    static BufferedImage orienter(BufferedImage image, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return image;
        }
        int l = image.getWidth();
        int h = image.getHeight();
        // x' = m00·x + m01·y + m02 ; y' = m10·x + m11·y + m12
        AffineTransform t = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, l, 0);   // miroir horizontal
            case 3 -> new AffineTransform(-1, 0, 0, -1, l, h);  // 180°
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);   // miroir vertical
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);    // transposition
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);   // 90° horaire
            case 7 -> new AffineTransform(0, -1, -1, 0, h, l);  // transposition inverse
            default -> new AffineTransform(0, -1, 1, 0, 0, l);  // 8 : 90° antihoraire
        };
        boolean permute = orientation >= 5;
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage cible = new BufferedImage(permute ? h : l, permute ? l : h, type);
        Graphics2D g = cible.createGraphics();
        try {
            g.drawImage(image, t, null);
        } finally {
            g.dispose();
        }
        return cible;
    }

    private static int lire16(byte[] d, int i, boolean petitBoutiste) {
        int a = d[i] & 0xFF;
        int b = d[i + 1] & 0xFF;
        return petitBoutiste ? (b << 8) | a : (a << 8) | b;
    }

    private static int lire32(byte[] d, int i, boolean petitBoutiste) {
        return petitBoutiste
                ? (lire16(d, i + 2, true) << 16) | lire16(d, i, true)
                : (lire16(d, i, false) << 16) | lire16(d, i + 2, false);
    }
}
