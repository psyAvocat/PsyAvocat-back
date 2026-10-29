package com.psyavocat.service.support;

/**
 * Le questionnaire d'orientation psychologique : unique, créé automatiquement au démarrage
 * (voir {@code QuestionnaireOrientationInitializer}). L'administration n'en crée pas d'autre :
 * elle gère ses questions, qui lui sont toujours rattachées.
 */
public final class QuestionnaireOrientation {

    public static final String CODE = "ORIENTATION_PSYCHOLOGIQUE";
    public static final String TITRE = "Questionnaire d'orientation psychologique";
    public static final String TYPE = "PSYCHOLOGIQUE";

    private QuestionnaireOrientation() {
    }
}
