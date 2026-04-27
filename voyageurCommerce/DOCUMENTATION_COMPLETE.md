# Documentation Complete - Projet TSP

## 1) But du programme

Ce programme sert a tester plusieurs methodes de resolution du probleme du Voyageur de Commerce (TSP):

- construire un tour qui visite chaque ville une seule fois,
- revenir a la ville de depart,
- comparer les couts et les temps entre methodes.

Le projet est oriente comparaison experimentale:

- tests sur graphe partiel (k plus proches voisins),
- tests sur graphe complet,
- benchmark sur 100 iterations,
- analyse avec et sans amelioration locale 2-opt (`deuxOpt()`).

## 1.1) Conventions de nommage

Pour eviter les ambiguities entre theorie et implementation, cette documentation utilise les conventions suivantes:

- `deuxOpt()` = nom de la methode Java, "2-opt" = nom de l heuristique.
- `minimumMatching(mst)` = nom de la methode Java, "MM" = abrevation courte dans les tableaux.
- `mstApprox()` et `christofides()` = noms de methodes Java pour les pipelines TSP correspondants.
- "MST" = arbre couvrant minimal (resultat de `kruskal()`).

## 2) Fichiers du code

- `voyageurCommerce/src/App.java`: point d entree, configuration et orchestration des tests.
- `voyageurCommerce/src/Graphe.java`: structure de graphe + algorithmes.
- `voyageurCommerce/src/Noeud.java`: noeud (ville) et calcul des distances.
- `voyageurCommerce/src/Arc.java`: arc (route) entre deux noeuds.

## 3) Flux global d execution

Depuis `App.main`:

1. Choisir un CSV existant ou en generer un nouveau.
2. Charger les noeuds depuis le CSV.
3. Construire un graphe partiel puis executer les methodes.
4. Construire un graphe complet puis executer les methodes.
5. Lancer le benchmark sur 100 iterations.

Le meme jeu de donnees est utilise pour partiel et complet pour une comparaison juste.

## 4) Configuration (App.java)

Les constantes en haut de `App.java` pilotent tout:

- `NOM_FICHIER_CSV`: fichier de donnees a utiliser.
- `GENERER_CSV`: active/desactive la generation de donnees.
- `NOM_FICHIER_GENERE`, `NOMBRE_NOEUDS_GENERES`, `TYPE_CSV_GENERE`.
- `K_VOISINS_PARTIEL` pour le graphe partiel.
- drapeaux d affichage / benchmark (`AFFICHER_*`, `BENCHMARK_*`, `TESTER_CHRISTOFIDES`).

Le code detecte automatiquement le bon chemin `data/` selon le dossier courant.

## 5) Format des donnees CSV

Format attendu:

- 1ere ligne: `GEO` ou `2D`
- lignes suivantes: `id;coord1;coord2`

Exemple:

```text
GEO
1;16,47;96,10
2;20,11;92,63
```

Le parseur accepte virgule ou point decimal.

## 6) Distance calculee (point critique)

### 6.1 Mode 2D

Distance euclidienne classique:

- formule: $\sqrt{(x_1-x_2)^2 + (y_1-y_2)^2}$

### 6.2 Mode GEO

Le projet utilise le format TSPLIB GEO (pas le Haversine decimal standard):

- coord1 = latitude,
- coord2 = longitude,
- valeur stockee en format degres.minutes.

Dans `Noeud.distanceTo`, la conversion TSPLIB est appliquee avant la formule GEO TSPLIB.

C est ce qui permet d obtenir des ordres de grandeur coherents avec les jeux de donnees de cours.

## 7) Methode par methode

## 7.1 `glouton()`

Principe:

1. choisir un depart aleatoire,
2. aller au voisin non visite le plus proche,
3. repeter jusqu a visiter tous les noeuds,
4. fermer le cycle.

Pourquoi:

- tres simple,
- tres rapide,
- sert de baseline.

Limite:

- fortement dependant du depart,
- pas de garantie de proximite a l optimal.

## 7.2 `kruskal()`

Principe:

1. trier les arcs par poids,
2. ajouter les arcs sans former de cycle (Union-Find),
3. obtenir un arbre couvrant minimal (MST).

Pourquoi:

- base solide pour `mstApprox` et `christofides`.

Limite:

- ce n est pas un tour TSP (c est un arbre).

## 7.3 `mstApprox()`

Principe:

1. construire le MST,
2. faire un DFS,
3. supprimer les repetitions (shortcut),
4. fermer le cycle.

Pourquoi:

- methode deterministe,
- robuste,
- rapide.

Limite:

- approximation, pas optimum garanti au cas par cas.

## 7.4 `minimumMatching(mst)`

Principe:

1. extraire les noeuds de degre impair du MST,
2. calculer un perfect matching minimum exact via DP bitmask,
3. retourner ce matching.

Pourquoi:

- necessaire pour construire Christofides correctement.

Limite:

- complexite exponentielle,
- borne pratique imposee (max 22 noeuds impairs).

## 7.5 `christofides()`

Principe:

1. MST (Kruskal),
2. matching minimum sur noeuds impairs,
3. multigraphe eulerien (MST + matching),
4. circuit eulerien (Hierholzer),
5. evaluation de plusieurs shortcuts (rotations + sens inverse) et selection du meilleur tour hamiltonien.

Pourquoi:

- methode de reference en approximation TSP metrique,
- souvent meilleure qu un glouton standard.

Limite:

- depend de la faisabilite du matching exact (borne 22 impairs).

Note implementation:

- dans ce projet, Christofides est rendu deterministe (depart eulerien fixe + ordre fixe + meilleur shortcut choisi de facon deterministe),
- sur une meme instance, il retourne donc le meme cout a chaque execution.

## 8) Amelioration locale 2-opt

Le projet inclut `deuxOpt(tour)`:

- prend un tour existant,
- echange des segments pour reduire le cout,
- repete jusqu a stabilisation (avec limite de passes).

Pourquoi:

- amelioration simple et efficace,
- permet de rapprocher nettement un tour d une solution optimale.

Le benchmark distingue:

- methode de base,
- methode + 2-opt.

## 9) Benchmark (`evaluerComplexite`)

Benchmark sur 100 iterations:

- temps moyen glouton,
- temps moyen glouton + 2-opt,
- temps moyen mstApprox,
- temps moyen mstApprox + 2-opt,
- temps moyen christofides,
- temps moyen christofides + 2-opt (si active).

Couts affiches (version actuelle):

- glouton: taux de reussite + meilleur/pire/moyen (car aleatoire),
- glouton + 2-opt: taux de reussite + meilleur/pire/moyen,
- mstApprox: un cout unique,
- mstApprox + 2-opt: un cout unique,
- christofides: un cout unique,
- christofides + 2-opt: un cout unique.

Affichage console:

- un tableau ASCII pour les temps,
- un tableau ASCII pour les resultats.

Note importante:

- pour les grands `n`, la mesure 2-opt est coupee automatiquement (`MAX_NOEUDS_2OPT_BENCH`) pour eviter des temps trop longs.

## 10) Ce que le programme fait bien

- gestion des CSV de cours en GEO TSPLIB,
- pipeline complet TSP (heuristique + approximation),
- benchmark compare base vs + 2-opt,
- execution stable sur petites et moyennes tailles,
- configuration simple via constantes de `App.java`.

## 11) Limites actuelles

- pas de solveur exact global du TSP (donc pas de garantie de trouver l optimum),
- matching exact limite a 22 impairs,
- glouton tres dependant du depart,
- 2-opt reste un optimum local, pas global.

## 12) Pourquoi les resultats peuvent varier

- glouton utilise un depart aleatoire,
- 2-opt depend du tour de depart,
- mstApprox et christofides sont deterministes dans cette implementation,
- un algorithme deterministe peut etre parfois moins bon qu un meilleur run glouton,
- pour glouton, un run ne suffit pas: il faut lire les moyennes et les meilleurs/pires.

## 13) Utilisation rapide

Compilation:

```bash
cd voyageurCommerce
javac -d bin src/*.java
```

Execution:

```bash
java -cp bin App
```

Changer de scenario:

- modifier les constantes en haut de `App.java`.

