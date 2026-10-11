package com.psyavocat.service.support;

import org.hibernate.Hibernate;

/**
 * Accès au type réel d'une entité chargée en différé.
 *
 * Une relation {@code @ManyToOne(fetch = LAZY)} vers un type abstrait
 * ({@code Utilisateur}, {@code Professionnel}) est servie par un proxy Hibernate
 * de ce type abstrait : {@code proxy instanceof Avocat} est alors toujours faux.
 * Tout test de type sur une entité issue d'une relation passe donc par {@link #reel}.
 */
public final class Entites {

    private Entites() {
    }

    /** L'entité elle-même (chargée si besoin), jamais son proxy ; {@code null} reste {@code null}. */
    @SuppressWarnings("unchecked")
    public static <T> T reel(T entite) {
        return entite == null ? null : (T) Hibernate.unproxy(entite);
    }
}
