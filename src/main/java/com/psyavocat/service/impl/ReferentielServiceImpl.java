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
import com.psyavocat.repository.ResultatOrientationRepository;
import com.psyavocat.repository.SpecialiteRepository;
import com.psyavocat.service.ReferentielService;
import com.psyavocat.service.support.TypesProfessionnel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Référentiels gérés par l'administration : domaines, spécialités et catégories de besoin.
 *
 * Règle d'intégrité : chaque élément porte un type de professionnel (AVOCAT ou PSYCHOLOGUE)
 * enregistré et validé ; une spécialité appartient au même univers que son domaine et que
 * ses catégories de besoin. Aucune déduction à partir des libellés.
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
    private final RealtimeGateway realtimeGateway;

    public ReferentielServiceImpl(
            DomaineRepository domaineRepository,
            SpecialiteRepository specialiteRepository,
            CategorieBesoinRepository categorieBesoinRepository,
            PonderationOrientationRepository ponderationRepository,
            ContenuRepository contenuRepository,
            DossierRepository dossierRepository,
            ResultatOrientationRepository resultatOrientationRepository,
            RealtimeGateway realtimeGateway
    ) {
        this.domaineRepository = domaineRepository;
        this.specialiteRepository = specialiteRepository;
        this.categorieBesoinRepository = categorieBesoinRepository;
        this.ponderationRepository = ponderationRepository;
        this.contenuRepository = contenuRepository;
        this.dossierRepository = dossierRepository;
        this.resultatOrientationRepository = resultatOrientationRepository;
        this.realtimeGateway = realtimeGateway;
    }

    // =========================================================================
    // DOMAINES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DomaineDTO> getAllDomaines() {
        return domaineRepository.findAll().stream().map(this::toDomaineDto).toList();
    }

    @Override
    public DomaineDTO createDomaine(DomaineDTO dto) {
        Domaine domaine = new Domaine();
        domaine.setNom(texteObligatoire(dto.getNom(), "Le nom du domaine"));
        domaine.setDescription(dto.getDescription());
        domaine.setTypeProfessionnel(TypesProfessionnel.exiger(dto.getTypeProfessionnel(), "Domaine"));
        Domaine saved = domaineRepository.save(domaine);
        signaler();
        return toDomaineDto(saved);
    }

    @Override
    public DomaineDTO updateDomaine(String id, DomaineDTO dto) {
        Domaine domaine = domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine introuvable : " + id));
        String type = TypesProfessionnel.exiger(dto.getTypeProfessionnel(), "Domaine");
        if (!type.equals(domaine.getTypeProfessionnel())
                && domaine.getSpecialites().stream().anyMatch(s -> !type.equals(TypesProfessionnel.typeDe(s)))) {
            throw new ConflictException("Ce domaine contient des spécialités d'un autre univers : modifiez-les avant de changer son type.");
        }
        domaine.setNom(texteObligatoire(dto.getNom(), "Le nom du domaine"));
        domaine.setDescription(dto.getDescription());
        domaine.setTypeProfessionnel(type);
        Domaine saved = domaineRepository.save(domaine);
        signaler();
        return toDomaineDto(saved);
    }

    @Override
    public void deleteDomaine(String id) {
        Domaine domaine = domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine introuvable : " + id));

        long count = specialiteRepository.countByDomaineId(id);
        if (count > 0) {
            throw new BadRequestException("Impossible de supprimer ce domaine : " + count + " spécialité(s) y sont rattachée(s). Veuillez d'abord modifier ou supprimer ces spécialités.");
        }

        domaineRepository.delete(domaine);
        signaler();
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

    /** Type obligatoire et cohérent avec le domaine et les catégories de besoin. */
    private void appliquer(Specialite specialite, SpecialiteDTO dto) {
        specialite.setNom(texteObligatoire(dto.getNom(), "Le nom de la spécialité"));
        specialite.setDescription(dto.getDescription());

        Domaine domaine = null;
        if (StringUtils.hasText(dto.getDomaineId())) {
            domaine = domaineRepository.findById(dto.getDomaineId())
                    .orElseThrow(() -> new BadRequestException("Domaine inconnu"));
        }

        String type = TypesProfessionnel.normaliser(dto.getTypeProfessionnel());
        if (type == null && domaine != null) {
            type = TypesProfessionnel.normaliser(domaine.getTypeProfessionnel());
        }
        if (type == null) {
            throw new BadRequestException("Indiquez l'univers de la spécialité (AVOCAT ou PSYCHOLOGUE).");
        }
        if (domaine != null && domaine.getTypeProfessionnel() != null && !type.equals(domaine.getTypeProfessionnel())) {
            throw new BadRequestException("Le domaine « " + domaine.getNom() + " » appartient à l'univers "
                    + domaine.getTypeProfessionnel() + " : la spécialité doit être du même univers.");
        }
        specialite.setTypeProfessionnel(type);
        specialite.setDomaine(domaine);

        if (dto.getCategorieIds() != null) {
            List<CategorieBesoin> categories = categorieBesoinRepository.findAllById(dto.getCategorieIds());
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
            list = type != null
                    ? categorieBesoinRepository.findAll().stream()
                        .filter(c -> type.equalsIgnoreCase(c.getTypeProfessionnel()))
                        .toList()
                    : categorieBesoinRepository.findAll();
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
        categorie.setTypeProfessionnel(TypesProfessionnel.exiger(dto.getTypeProfessionnel(), "Catégorie de besoin"));
        categorie.setActif(dto.getActif() != null ? dto.getActif() : true);
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        signaler();
        return toCategorieDto(saved);
    }

    @Override
    public CategorieBesoinDTO updateCategorieBesoin(String id, CategorieBesoinDTO dto) {
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        String type = TypesProfessionnel.exiger(dto.getTypeProfessionnel(), "Catégorie de besoin");
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
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        categorie.setActif(categorie.getActif() == null || !categorie.getActif());
        CategorieBesoin saved = categorieBesoinRepository.save(categorie);
        signaler();
        return toCategorieDto(saved);
    }

    /** « Suppression » = désactivation : l'historique des résultats d'orientation est préservé. */
    @Override
    public void deleteCategorieBesoin(String id) {
        CategorieBesoin categorie = categorieBesoinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        categorie.setActif(false);
        categorieBesoinRepository.save(categorie);
        signaler();
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
