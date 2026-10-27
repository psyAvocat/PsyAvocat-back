package com.psyavocat.service.impl;

import com.psyavocat.dto.referentiel.CategorieBesoinDTO;
import com.psyavocat.dto.referentiel.DomaineDTO;
import com.psyavocat.dto.referentiel.SpecialiteDTO;
import com.psyavocat.entity.CategorieBesoin;
import com.psyavocat.entity.Domaine;
import com.psyavocat.entity.Specialite;
import com.psyavocat.exception.BadRequestException;
import com.psyavocat.exception.ConflictException;
import com.psyavocat.exception.ResourceNotFoundException;
import com.psyavocat.realtime.RealtimeGateway;
import com.psyavocat.repository.CategorieBesoinRepository;
import com.psyavocat.repository.ContenuRepository;
import com.psyavocat.repository.DomaineRepository;
import com.psyavocat.repository.DossierRepository;
import com.psyavocat.repository.PonderationOrientationRepository;
import com.psyavocat.repository.ResultatOrientationCategorieRepository;
import com.psyavocat.repository.ResultatOrientationRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.ReferentielService;
import com.psyavocat.service.support.DomainesFixes;
import com.psyavocat.service.support.TypesProfessionnel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Référentiels gérés par l'administration : domaines, spécialités et catégories de besoin.
 *
 * Règles :
 * - deux domaines fixes, un par univers (« Psychologique », « Juridique »), créés au démarrage :
 *   l'administration ne les crée ni ne les supprime, chaque spécialité est rattachée au domaine
 *   de son univers ;
 * - les catégories de besoin n'existent que pour l'univers Psychologue ;
 * - chaque élément porte un type de professionnel enregistré et validé, jamais déduit d'un libellé.
 */
@Service
@Transactional
public class ReferentielServiceImpl implements ReferentielService {

    private final DomaineRepository domaineRepository;
    private final SpecialiteRepository specialiteRepository;
    private final CategorieBesoinRepository categorieBesoinRepository;
    private final PonderationOrientationRepository ponderationRepository;
    private final ContenuRepository contenuRepository;
    private final DossierRepository dossierRepository;
    private final ResultatOrientationRepository resultatOrientationRepository;
    private final ResultatOrientationCategorieRepository resultatOrientationCategorieRepository;
    private final RealtimeGateway realtimeGateway;

    public ReferentielServiceImpl(
            DomaineRepository domaineRepository,
            SpecialiteRepository specialiteRepository,
            CategorieBesoinRepository categorieBesoinRepository,
            PonderationOrientationRepository ponderationRepository,
            ContenuRepository contenuRepository,
            DossierRepository dossierRepository,
            ResultatOrientationRepository resultatOrientationRepository,
            ResultatOrientationCategorieRepository resultatOrientationCategorieRepository,
            RealtimeGateway realtimeGateway
    ) {
        this.domaineRepository = domaineRepository;
        this.specialiteRepository = specialiteRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
        this.ponderationRepository = ponderationRepository;
        this.contenuRepository = contenuRepository;
        this.dossierRepository = dossierRepository;
        this.resultatOrientationRepository = resultatOrientationRepository;
        this.resultatOrientationCategorieRepository = resultatOrientationCategorieRepository;
        this.realtimeGateway = realtimeGateway;
    }

    // =========================================================================
    // DOMAINES
    // =========================================================================

    /** Les deux domaines fixes (Psychologique, puis Juridique). */
    @Override
    @Transactional(readOnly = true)
    public List<DomaineDTO> getAllDomaines() {
        return java.util.Arrays.stream(DomainesFixes.values())
                .map(f -> domaineRepository.findByCode(f.code()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::toDomaineDto)
                .toList();
    }

    /** Domaines fixes : seule la description est modifiable (nom et univers ne changent pas). */
    @Override
    public DomaineDTO updateDomaine(String id, DomaineDTO dto) {
        Domaine domaine = domaineRepository.findById(id)
                .filter(d -> d.getCode() != null)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine introuvable : " + id));
        domaine.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        Domaine saved = domaineRepository.save(domaine);
        signaler();
        return toDomaineDto(saved);
    }

    /** Domaine fixe d'un univers (créé au démarrage par DomainesFixesInitializer). */
    private Domaine domaineDeLUnivers(String type) {
        DomainesFixes fixe = DomainesFixes.pourType(type);
        return fixe == null ? null : domaineRepository.findByCode(fixe.code()).orElse(null);
    }

    // =========================================================================
    // SPÉCIALITÉS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SpecialiteDTO> getAllSpecialites() {
        return specialiteRepository.findAll().stream()
                .map(this::mapToSpecialiteDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialiteDTO> getSpecialites(String typeProfessionnel) {
        String type = TypesProfessionnel.normaliser(typeProfessionnel);
        if (type == null) {
            return getAllSpecialites();
        }
        return specialiteRepository.findByTypeProfessionnel(type).stream()
                .map(this::mapToSpecialiteDTO)
                .toList();
    }

    @Override
    public SpecialiteDTO createSpecialite(SpecialiteDTO dto) {
        Specialite specialite = new Specialite();
        appliquer(specialite, dto);
        Specialite saved = specialiteRepository.save(specialite);
        signaler();
        return mapToSpecialiteDTO(saved);
    }

    @Override
    public SpecialiteDTO updateSpecialite(String id, SpecialiteDTO dto) {
        Specialite specialite = specialiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + id));
        String ancienType = TypesProfessionnel.typeDe(specialite);
        appliquer(specialite, dto);
        String nouveauType = TypesProfessionnel.typeDe(specialite);
        if (ancienType != null && !ancienType.equals(nouveauType)
                && specialiteRepository.countProfessionnels(id) > 0) {
            throw new ConflictException("Des professionnels de l'univers " + ancienType
                    + " utilisent cette spécialité : son univers ne peut pas être changé.");
        }
        Specialite saved = specialiteRepository.save(specialite);
        signaler();
        return mapToSpecialiteDTO(saved);
    }

    /**
     * Suppression uniquement si la spécialité n'est référencée nulle part : sinon l'historique
     * (profils, orientations, contenus, dossiers) serait corrompu.
     */
    @Override
    public void deleteSpecialite(String id) {
        Specialite specialite = specialiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spécialité introuvable : " + id));
        long pros = specialiteRepository.countProfessionnels(id);
        if (pros > 0) {
            throw new ConflictException("Impossible de supprimer cette spécialité : " + pros + " professionnel(s) l'utilisent.");
        }
        if (ponderationRepository.existsBySpecialiteId(id) || resultatOrientationRepository.existsBySpecialiteId(id)) {
            throw new ConflictException("Impossible de supprimer cette spécialité : elle est utilisée par l'orientation.");
        }
        if (contenuRepository.existsBySpecialiteId(id) || dossierRepository.existsBySpecialiteId(id)) {
            throw new ConflictException("Impossible de supprimer cette spécialité : des publications ou dossiers y sont rattachés.");
        }
        specialiteRepository.delete(specialite);
        signaler();
    }

    /**
     * Univers obligatoire ; le domaine est celui de l'univers (fixe) et les catégories de besoin,
     * réservées aux psychologues, doivent être du même univers.
     */
    private void appliquer(Specialite specialite, SpecialiteDTO dto) {
        specialite.setNom(texteObligatoire(dto.getNom(), "Le nom de la spécialité"));
        specialite.setDescription(dto.getDescription());

        String type = TypesProfessionnel.normaliser(dto.getTypeProfessionnel());
        if (type == null) {
            throw new BadRequestException("Indiquez l'univers de la spécialité (AVOCAT ou PSYCHOLOGUE).");
        }
        specialite.setTypeProfessionnel(type);
        specialite.setDomaine(domaineDeLUnivers(type));

        if (TypesProfessionnel.AVOCAT.equals(type)) {
            // Pas de catégorie de besoin dans l'univers Avocat.
            specialite.setCategoriesBesoin(new java.util.ArrayList<>());
        } else if (dto.getCategorieIds() != null) {
            List<CategorieBesoin> categories = categorieBesoinRepository.findAllById(dto.getCategorieIds()).stream()
                    .filter(c -> !estSupprimee(c))
                    .toList();
            if (categories.size() != dto.getCategorieIds().stream().distinct().count()) {
                throw new BadRequestException("Certaines catégories de besoin sont introuvables.");
            }
            for (CategorieBesoin c : categories) {
                if (!type.equals(TypesProfessionnel.normaliser(c.getTypeProfessionnel()))) {
                    throw new BadRequestException("La catégorie « " + c.getNom() + " » n'appartient pas à l'univers " + type + ".");
                }
            }
            specialite.setCategoriesBesoin(categories);
        }
    }

    private DomaineDTO toDomaineDto(Domaine d) {
        return DomaineDTO.builder()
                .id(d.getId())
                .code(d.getCode())
                .nombreSpecialites(specialiteRepository.countByDomaineId(d.getId()))
                .nom(d.getNom())
                .description(d.getDescription())
                .typeProfessionnel(d.getTypeProfessionnel())
                .build();
    }

    private SpecialiteDTO mapToSpecialiteDTO(Specialite s) {
        SpecialiteDTO dto = new SpecialiteDTO();
        dto.setId(s.getId());
        dto.setNom(s.getNom());
        dto.setDescription(s.getDescription());
        dto.setTypeProfessionnel(TypesProfessionnel.typeDe(s));

        if (s.getDomaine() != null) {
            dto.setDomaineId(s.getDomaine().getId());
            dto.setDomaineNom(s.getDomaine().getNom());
        }

        if (s.getCategoriesBesoin() != null && !s.getCategoriesBesoin().isEmpty()) {
            dto.setCategorieIds(s.getCategoriesBesoin().stream().map(CategorieBesoin::getId).toList());
            dto.setCategories(s.getCategoriesBesoin().stream()
                    .map(this::toCategorieDto)
                    .toList());
        } else {
            dto.setCategorieIds(Collections.emptyList());
            dto.setCategories(Collections.emptyList());
        }

        return dto;
    }

    // =========================================================================
    // CATÉGORIES DE BESOIN
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel) {
        return getCategoriesBesoin(typeProfessionnel, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorieBesoinDTO> getCategoriesBesoin(String typeProfessionnel, boolean includeInactive) {
        String type = TypesProfessionnel.normaliser(typeProfessionnel);
        List<CategorieBesoin> list;
        if (includeInactive) {
            // Inactives comprises, mais jamais les catégories supprimées.
            list = categorieBesoinRepository.findAll().stream()
                    .filter(c -> !estSupprimee(c))
                    .filter(c -> type == null || type.equalsIgnoreCase(c.getTypeProfessionnel()))
                    .toList();
        } else {
            list = type != null
                    ? categorieBesoinRepository.findByActifTrueAndTypeProfessionnel(type)
                    : categorieBesoinRepository.findByActifTrue();
        }
        return list.stream().map(this::toCategorieDto).toList();
    }

    @Override
    public CategorieBesoinDTO createCategorieBesoin(CategorieBesoinDTO dto) {
        CategorieBesoin categorie = new CategorieBesoin();
        categorie.setNom(texteObligatoire(dto.getNom(), "Le nom de la catégorie"));
        categorie.setDescription(dto.getDescription());
        categorie.setTypeProfessionnel(typeCategorie(dto.getTypeProfessionnel()));
        categorie.setActif(dto.getActif() != null ? dto.getActif() : true);
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        signaler();
        return toCategorieDto(saved);
    }

    @Override
    public CategorieBesoinDTO updateCategorieBesoin(String id, CategorieBesoinDTO dto) {
        CategorieBesoin categorie = categorieExistante(id);
        String type = typeCategorie(dto.getTypeProfessionnel());
        if (!type.equals(TypesProfessionnel.normaliser(categorie.getTypeProfessionnel()))
                && categorie.getSpecialites().stream().anyMatch(s -> !type.equals(TypesProfessionnel.typeDe(s)))) {
            throw new ConflictException("Des spécialités d'un autre univers sont liées à cette catégorie : retirez-les avant de changer son type.");
        }
        categorie.setNom(texteObligatoire(dto.getNom(), "Le nom de la catégorie"));
        categorie.setDescription(dto.getDescription());
        categorie.setTypeProfessionnel(type);
        if (dto.getActif() != null) {
            categorie.setActif(dto.getActif());
        }
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        signaler();
        return toCategorieDto(saved);
    }

    @Override
    public CategorieBesoinDTO toggleActifCategorieBesoin(String id) {
        CategorieBesoin categorie = categorieExistante(id);
        categorie.setActif(categorie.getActif() == null || !categorie.getActif());
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        signaler();
        return toCategorieDto(saved);
    }

    /**
     * Suppression définitive d'une catégorie de besoin (distincte de la désactivation).
     *
     * - Refusée tant que des pondérations du questionnaire l'utilisent : elles doivent être
     *   retirées d'abord, sinon l'orientation continuerait de la calculer.
     * - Retirée des spécialités qui la référencent.
     * - Effacée de la base ; seule exception : si des résultats d'orientation passés y font
     *   référence, elle est conservée pour cet historique mais marquée supprimée et n'apparaît
     *   plus nulle part (listes actives et inactives, formulaires).
     */
    @Override
    public void deleteCategorieBesoin(String id) {
        CategorieBesoin categorie = categorieExistante(id);

        long ponderations = ponderationRepository.countByCategorieBesoinId(id);
        if (ponderations > 0) {
            throw new ConflictException("Impossible de supprimer cette catégorie : " + ponderations
                    + " pondération(s) du questionnaire d'orientation l'utilisent. Retirez-les d'abord, ou désactivez la catégorie.");
        }

        for (Specialite specialite : List.copyOf(categorie.getSpecialites())) {
            specialite.getCategoriesBesoin().removeIf(c -> id.equals(c.getId()));
            specialiteRepository.save(specialite);
        }
        categorie.getSpecialites().clear();

        boolean historique = resultatOrientationRepository.existsByCategorieBesoinId(id)
                || resultatOrientationCategorieRepository.existsByCategorieBesoinId(id);
        if (historique) {
            categorie.setActif(false);
            categorie.setSupprime(true);
            categorieBesoinRepository.save(categorie);
        } else {
            categorieBesoinRepository.delete(categorie);
        }
        signaler();
    }

    /** Les catégories de besoin n'existent que pour l'univers Psychologue (type facultatif à la saisie). */
    private String typeCategorie(String valeur) {
        String type = TypesProfessionnel.normaliser(valeur);
        if (TypesProfessionnel.AVOCAT.equals(type)) {
            throw new BadRequestException("Les catégories de besoin sont réservées à l'univers Psychologue.");
        }
        return TypesProfessionnel.PSYCHOLOGUE;
    }

    /** Catégorie non supprimée, sinon « introuvable ». */
    private CategorieBesoin categorieExistante(String id) {
        return categorieBesoinRepository.findById(id)
                .filter(c -> !estSupprimee(c))
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
    }

    private static boolean estSupprimee(CategorieBesoin c) {
        return Boolean.TRUE.equals(c.getSupprime());
    }

    private CategorieBesoinDTO toCategorieDto(CategorieBesoin c) {
        return new CategorieBesoinDTO(c.getId(), c.getNom(), c.getDescription(), c.getTypeProfessionnel(), c.getActif());
    }

    private String texteObligatoire(String valeur, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new BadRequestException(libelle + " est obligatoire.");
        }
        return valeur.trim();
    }

    /** Les clients connectés rechargent leurs filtres et référentiels. */
    private void signaler() {
        realtimeGateway.diffuser("REFERENTIEL_MIS_A_JOUR", Map.of());
    }
}
