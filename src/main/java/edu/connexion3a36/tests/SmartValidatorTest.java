package edu.connexion3a36.tests;

import edu.connexion3a36.services.SmartValidator;

/**
 * SmartValidatorTest
 * ══════════════════
 * Tests rapides sans JavaFX — lancez avec :
 *   Clic droit → Run 'SmartValidatorTest.main()'
 *
 * Nécessite GROQ_API_KEY dans .env ou variable d'environnement.
 */
public class SmartValidatorTest {

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════");
        System.out.println("  SmartValidator — Tests sémantiques");
        System.out.println("═══════════════════════════════════════\n");

        // Cas de test : {attendue, donnee, attendu_correct}
        Object[][] cas = {
            // ── Devrait être VRAI ──────────────────────────────────────────
            { "Paris",    "Paris est la capitale de la France",          true  },
            { "Paris",    "C'est Paris",                                 true  },
            { "Paris",    "paris",                                       true  },
            { "Newton",   "Isaac Newton a découvert la loi de gravité",  true  },
            { "l'eau",    "H2O est la formule de l'eau",                 true  },
            { "22",       "La réponse est 22",                           true  },

            // ── Devrait être FAUX ──────────────────────────────────────────
            { "Paris",    "Londres est la capitale de l'Angleterre",     false },
            { "Newton",   "Einstein a tout inventé",                     false },
            { "22",       "Je ne sais pas",                              false },
            { "Paris",    "",                                            false },
        };

        int ok = 0, erreurs = 0;

        for (Object[] cas_ : cas) {
            String  attendue = (String)  cas_[0];
            String  donnee   = (String)  cas_[1];
            boolean attenduC = (boolean) cas_[2];

            SmartValidator.Result r = SmartValidator.valider(attendue, donnee);

            String statut;
            if (r.correct() == attenduC) {
                statut = "✅ PASS";
                ok++;
            } else {
                statut = "❌ FAIL";
                erreurs++;
            }

            System.out.printf(
                "%s | attendue=«%s» | donnée=«%s»%n       → %s | %s | source=%s%n%n",
                statut, attendue, donnee,
                r.correct() ? "CORRECT" : "INCORRECT",
                r.explication(),
                r.source()
            );
        }

        System.out.println("═══════════════════════════════════════");
        System.out.printf("  Résultat : %d/%d tests réussis%n", ok, cas.length);
        if (erreurs == 0) {
            System.out.println("  🎉 Tous les tests sont passés !");
        } else {
            System.out.println("  ⚠️  " + erreurs + " test(s) échoué(s)");
        }
        System.out.println("═══════════════════════════════════════");
    }
}
