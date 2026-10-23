package com.psyavocat.storage.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageOptimizerTest {

    /** Image « photo » : dégradé + bruit, pour un poids réaliste. */
    private static BufferedImage photo(int largeur, int hauteur, int type) {
        BufferedImage img = new BufferedImage(largeur, hauteur, type);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(30, 60, 120), largeur, hauteur, new Color(220, 180, 90)));
        g.fillRect(0, 0, largeur, hauteur);
        Random r = new Random(42);
        for (int i = 0; i < largeur * hauteur / 40; i++) {
            img.setRGB(r.nextInt(largeur), r.nextInt(hauteur), r.nextInt());
        }
        g.dispose();
        return img;
    }

    private static byte[] encoder(BufferedImage img, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static BufferedImage lire(byte[] octets) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(octets));
    }

    @Test
    @DisplayName("Photo de téléphone (4000x3000) : réduite à 800 px pour un avatar, et plus légère")
    void grandePhoto_reduite() throws Exception {
        byte[] original = encoder(photo(4000, 3000, BufferedImage.TYPE_INT_RGB), "jpg");

        ImageOptimizer.Resultat r = ImageOptimizer.optimiser(original, "image/jpeg", "jpg", ImageOptimizer.tailleMax("avatars"));

        BufferedImage resultat = lire(r.octets());
        assertThat(Math.max(resultat.getWidth(), resultat.getHeight())).isEqualTo(800);
        assertThat(resultat.getWidth()).isEqualTo(800);
        assertThat(resultat.getHeight()).isEqualTo(600); // proportions conservées
        assertThat(r.octets().length).isLessThan(original.length);
        assertThat(r.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Couverture d'article : limitée à 1600 px")
    void couverture_limiteeA1600() throws Exception {
        byte[] original = encoder(photo(3200, 1800, BufferedImage.TYPE_INT_RGB), "jpg");

        ImageOptimizer.Resultat r = ImageOptimizer.optimiser(original, "image/jpeg", "jpg", ImageOptimizer.tailleMax("contenus"));

        assertThat(lire(r.octets()).getWidth()).isEqualTo(1600);
    }

    @Test
    @DisplayName("PNG opaque converti en JPEG ; PNG transparent conservé en PNG")
    void png() throws Exception {
        byte[] opaque = encoder(photo(2000, 2000, BufferedImage.TYPE_INT_RGB), "png");
        ImageOptimizer.Resultat r1 = ImageOptimizer.optimiser(opaque, "image/png", "png", 800);
        assertThat(r1.contentType()).isEqualTo("image/jpeg");
        assertThat(r1.extension()).isEqualTo("jpg");

        byte[] transparent = encoder(photo(2000, 2000, BufferedImage.TYPE_INT_ARGB), "png");
        ImageOptimizer.Resultat r2 = ImageOptimizer.optimiser(transparent, "image/png", "png", 800);
        assertThat(r2.contentType()).isEqualTo("image/png");
        assertThat(lire(r2.octets()).getColorModel().hasAlpha()).isTrue();
        assertThat(lire(r2.octets()).getWidth()).isEqualTo(800);
    }

    @Test
    @DisplayName("Image déjà légère et petite : l'original est gardé tel quel")
    void petiteImage_inchangee() throws Exception {
        BufferedImage uni = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        byte[] original = encoder(uni, "jpg");

        ImageOptimizer.Resultat r = ImageOptimizer.optimiser(original, "image/jpeg", "jpg", 800);

        assertThat(r.octets()).isSameAs(original);
    }

    /** Insère un segment EXIF (APP1) portant l'orientation donnée juste après l'en-tête JPEG. */
    private static byte[] avecOrientation(byte[] jpeg, int orientation) {
        byte[] app1 = {
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,                 // APP1, longueur 34
                'E', 'x', 'i', 'f', 0x00, 0x00,                        // « Exif »
                'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,          // TIFF gros-boutiste, IFD0 en 8
                0x00, 0x01,                                            // 1 entrée
                0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,        // orientation, SHORT, 1 valeur
                0x00, (byte) orientation, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00                                 // pas d'IFD suivant
        };
        byte[] resultat = new byte[jpeg.length + app1.length];
        System.arraycopy(jpeg, 0, resultat, 0, 2);
        System.arraycopy(app1, 0, resultat, 2, app1.length);
        System.arraycopy(jpeg, 2, resultat, 2 + app1.length, jpeg.length - 2);
        return resultat;
    }

    @Test
    @DisplayName("Photo de téléphone tournée (EXIF 6) : remise à l'endroit, en portrait")
    void orientationExif_appliquee() throws Exception {
        byte[] paysage = encoder(photo(2000, 1000, BufferedImage.TYPE_INT_RGB), "jpg");
        byte[] tournee = avecOrientation(paysage, 6);
        assertThat(ImageOptimizer.orientationExif(tournee)).isEqualTo(6);
        assertThat(ImageOptimizer.orientationExif(paysage)).isEqualTo(1);

        BufferedImage resultat = lire(ImageOptimizer.optimiser(tournee, "image/jpeg", "jpg", 800).octets());

        assertThat(resultat.getWidth()).isEqualTo(400);
        assertThat(resultat.getHeight()).isEqualTo(800);
    }

    @Test
    @DisplayName("Rotations EXIF : dimensions et coin haut-gauche correctement déplacés")
    void orientations_toutes() {
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFF0000); // repère rouge en haut à gauche
        for (int o = 1; o <= 8; o++) {
            BufferedImage r = ImageOptimizer.orienter(img, o);
            boolean permute = o >= 5;
            assertThat(r.getWidth()).as("largeur, orientation %d", o).isEqualTo(permute ? 2 : 4);
            assertThat(r.getHeight()).as("hauteur, orientation %d", o).isEqualTo(permute ? 4 : 2);
        }
        // 90° horaire : le coin haut-gauche part en haut à droite.
        assertThat(ImageOptimizer.orienter(img, 6).getRGB(1, 0) & 0xFFFFFF).isEqualTo(0xFF0000);
        // 180° : il part en bas à droite.
        assertThat(ImageOptimizer.orienter(img, 3).getRGB(3, 1) & 0xFFFFFF).isEqualTo(0xFF0000);
        // 90° antihoraire : il part en bas à gauche.
        assertThat(ImageOptimizer.orienter(img, 8).getRGB(0, 3) & 0xFFFFFF).isEqualTo(0xFF0000);
    }

    @Test
    @DisplayName("Format illisible (WEBP, fichier corrompu) : envoi de l'original sans erreur")
    void illisible_inchange() {
        byte[] faux = "pas une image".getBytes();

        assertThat(ImageOptimizer.optimiser(faux, "image/jpeg", "jpg", 800).octets()).isSameAs(faux);
        assertThat(ImageOptimizer.optimiser(faux, "image/webp", "webp", 800).octets()).isSameAs(faux);
    }
}
