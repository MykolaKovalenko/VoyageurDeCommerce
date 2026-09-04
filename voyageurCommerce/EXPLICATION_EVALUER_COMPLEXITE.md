# Explication detaillee - evaluerComplexite

## Objectif de la methode

La methode evaluerComplexite sert a mesurer les performances et la qualite de plusieurs approches TSP sur la meme instance.

Elle calcule :
- des temps moyens d execution (en secondes),
- des statistiques de cout (meilleur, pire, moyenne),
- l effet de deuxOpt sur plusieurs methodes.

Elle fait ces mesures sur 100 repetitions pour lisser les variations.

---

## Signature et parametres

Signature :
public void evaluerComplexite(boolean benchmarkKruskal, boolean benchmarkMM, boolean benchmarkChristofides)

Role des 3 booleens :
- benchmarkKruskal : active ou non la mesure du temps de kruskal seul.
- benchmarkMM : active ou non la mesure de minimumMatching seul et du pipeline MST + MM.
- benchmarkChristofides : active ou non la mesure de christofides.

Important : glouton et mstApprox sont toujours mesures. Les autres blocs sont conditionnels.

---

## Etape 1 - Initialisation globale

La methode lit d abord :
- n = nombre de noeuds du graphe,
- repetitions = 100,
- mesurer2Opt = vrai si n <= MAX_NOEUDS_2OPT_BENCH.

Interpretation :
- quand n est trop grand, deuxOpt est ignore volontairement pour eviter des temps trop longs.

Ensuite elle initialise beaucoup d accumulateurs :
- totaux de temps en nanosecondes (long),
- stats de cout (best, worst, somme, compteurs valides).

Pourquoi des compteurs valides :
- certaines methodes peuvent retourner null dans certains cas,
- on ne veut pas diviser par 100 si certaines iterations sont invalides.

---

## Etape 2 - Bloc glouton (toujours execute)

Boucle de 100 iterations :
1. mesurer le temps de glouton,
2. si le tour est valide, accumuler le temps dans totalGlouton,
3. si le tour est valide :
   - mettre a jour meilleur/pire cout,
   - ajouter le cout a la somme,
   - incrementer nbGloutonValides.

Important : les runs qui echouent (tour null) ne sont pas comptes dans totalGlouton, pour garder un temps moyen coherent avec le nombre de succes.

Puis, si mesurer2Opt est vrai :
- appliquer deuxOpt sur le tour glouton,
- mesurer le temps de deuxOpt seul,
- accumuler ses statistiques de cout si le resultat est valide.

Ce que ca permet :
- comparer glouton brut vs glouton ameliore par deuxOpt.

---

## Etape 3 - Bloc kruskal seul (optionnel)

Si benchmarkKruskal est vrai :
- 100 iterations,
- mesurer uniquement le temps de this.kruskal(),
- stocker dans totalKruskal.

Remarque :
- ici on mesure la construction de MST, pas un tour TSP final.

---

## Etape 4 - Bloc mstApprox (toujours execute)

Boucle de 100 iterations :
1. mesurer le temps de mstApprox,
2. accumuler dans totalMstApprox.

Si mesurer2Opt et tour valide :
- appliquer deuxOpt,
- mesurer le temps de deuxOpt seul,
- stocker la somme des couts pour la moyenne.

Ce bloc compare :
- mstApprox brut,
- mstApprox + deuxOpt.

---

## Etape 5 - Blocs minimumMatching (optionnels)

Si benchmarkMM est vrai, il y a deux mesures distinctes.

Mesure A - minimumMatching seul :
- faire kruskal avant chrono (hors mesure),
- chrono uniquement sur minimumMatching(mst),
- stocker dans totalMM.

Pourquoi utile :
- isole le cout propre du matching sans polluer avec kruskal.

Mesure B - pipeline MST + MM :
- chrono autour de kruskal puis minimumMatching,
- stocker dans totalMstPlusMM.

Pourquoi utile :
- donne le cout reel si on enchaine les deux en pratique.

---

## Etape 6 - Bloc christofides (optionnel)

Si benchmarkChristofides est vrai :
- 100 iterations de christofides,
- accumulation du temps dans totalChristofides.

Si mesurer2Opt et tour valide :
- appliquer deuxOpt a ce tour,
- mesurer le temps de deuxOpt seul,
- accumuler les couts pour la moyenne.

But :
- comparer christofides brut vs christofides + deuxOpt.

---

## Etape 7 - Conversion nanosecondes vers secondes

Pour la plupart des methodes, les totaux sont convertis en temps moyen par :
- total / repetitions / 1_000_000_000.0

**Exception pour glouton :** le temps est divise par `nbGloutonValides` (le nombre de succes) et non par `repetitions`. En effet, sur un graphe partiel, glouton peut echouer (retourner null) pour certains departs aleatoires. Diviser par 100 inclurait le temps des echecs et fausserait la mesure du cout reel d'une execution reussie. Protection contre la division par zero : si `nbGloutonValides == 0`, le temps affiche est n/a.

Meme logique pour glouton + 2-opt : divise par `nbGlouton2OptValides`.

Variables produites :
- sGlouton, sKruskal, sMstApprox, sMM, sMstPlusMM, sChristofides,
- plus les versions deuxOpt seules,
- puis les versions combinees (ex: sGlouton2Opt = sGlouton + sGlouton2OptSeul).

Idee cle :
- la methode affiche a la fois le temps de base et le temps total avec deuxOpt.

---

## Etape 8 - Affichage des temps

La console affiche un bloc Evaluation des temps avec :
- glouton,
- glouton + 2-opt (si active),
- kruskal (si demande),
- mstApprox,
- mstApprox + 2-opt (si active),
- minimumMatching seul (si demande),
- pipeline MST + MM (si demande),
- christofides (si demande),
- christofides + 2-opt (si possible).

Si deuxOpt est desactive (n trop grand), message explicite :
2-opt non mesure (n > MAX_NOEUDS_2OPT_BENCH).

---

## Etape 9 - Affichage des qualites de solution

La methode affiche ensuite :
- Resultats glouton : taux de reussite, meilleur, pire, moyenne.

Le taux de reussite indique combien d'iterations glouton ont produit un tour valide sur les 100 :
- format : X/100 (Y%)
- sur graphe complet : toujours 100/100 (100%)
- sur graphe partiel : peut etre inferieur, selon la structure du graphe et les departs aleatoires

Puis, si deuxOpt est actif :
- Resultats glouton + 2-opt : taux de reussite, meilleur, pire, moyenne.
- mstApprox : un cout unique.
- mstApprox + 2-opt : un cout unique.
- christofides (si active) : un cout unique.
- christofides + 2-opt (si active) : un cout unique.

Gestion des cas invalides :
- si aucune valeur valide, la methode affiche n/a grace a Double.isFinite.

Forme de sortie :
- un tableau ASCII pour les temps,
- un tableau ASCII pour les resultats.

---

## Ce que la methode utilise

Dependances directes :
- glouton,
- deuxOpt,
- kruskal,
- mstApprox,
- minimumMatching,
- christofides,
- cout.

API de mesure :
- System.nanoTime pour chronometrer,
- System.out.println et System.out.printf pour afficher.

---

## Comment lire les resultats rapidement

Lecture recommandee :
1. Regarder le taux de reussite glouton : sur graphe partiel, un taux bas indique que le filtre de sortie rejette beaucoup de chemins depuis certains departs.
2. Regarder les temps moyens : quelle methode est la plus rapide (pour glouton, temps par succes uniquement).
3. Comparer les couts uniques de mstApprox/christofides entre eux et avec glouton moyen.
4. Regarder l impact de deuxOpt : gain de qualite vs surcout en temps.

Regle pratique :
- si deuxOpt reduit fortement le cout pour un surcout raisonnable, il est rentable.
- si n est grand, son cout peut devenir trop eleve (d ou la coupure automatique).
- un taux de reussite glouton faible sur graphe partiel peut indiquer que k est trop petit pour la taille du graphe.

---

## Limites et interpretation

1. 100 repetitions donnent une bonne tendance, mais pas une mesure statistique complete (pas d ecart-type affiche).
2. Glouton est aleatoire, donc les stats meilleur/pire/moyenne sont importantes.
3. minimumMatching exact peut etre tres couteux sur gros cas, ce qui impacte Christofides.
4. Les temps incluent l overhead Java runtime normal (JIT, GC possibles).

---

## Resume en une phrase

evaluerComplexite est un mini banc d essai integre qui compare vitesse et qualite de plusieurs pipelines TSP, avec ou sans deuxOpt, en affichant des tableaux lisibles et des stats detaillees seulement pour la partie aleatoire (glouton).
