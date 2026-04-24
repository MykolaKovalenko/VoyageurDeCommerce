# Explication détaillée — `deuxOpt()`

## Vue d'ensemble

`deuxOpt` est une méthode d'**amélioration locale** pour le TSP. Elle prend un tour déjà construit (par `glouton`, `mstApprox` ou `christofides`) et essaie de le raccourcir.

**Idée** : si deux arcs du tour se croisent, c'est forcément sous-optimal. On peut toujours les décroisser et gagner en distance. L'algorithme cherche donc toutes les paires d'arcs qui se croisent et les échange.

Elle ne construit pas un tour depuis zéro — elle **améliore** un tour existant.

---

## Pourquoi ça marche ?

Dans un plan euclidien, deux segments qui se croisent ont toujours une longueur totale plus grande que les deux segments qui relient les mêmes points sans se croiser (inégalité triangulaire).

Exemple simple :

```
Avant :  A ----> B          Après :  A ----> C
              X                          |
         C ----> D                   B ----> D
```

Si A-C + B-D < A-B + C-D, alors on gagne à faire l'échange.

---

## Pré-requis

- `deuxOpt(tour)` reçoit un graphe `tour` déjà construit.
- Le graphe source (`this`) doit contenir les noeuds originaux pour pouvoir calculer les distances.
- Si le tour a moins de 5 noeuds, il n'y a rien à améliorer (retour immédiat).

---

## Code annoté ligne par ligne

```java
public Graphe deuxOpt(Graphe tour) {
```
Retourne un nouveau `Graphe` représentant le tour amélioré.

---

### Étape 1 — Cas limites

```java
if (tour == null || tour.getNoeuds().size() < 5) {
    return tour;
}
```
En dessous de 5 noeuds, il n'existe pas deux paires d'arcs non-adjacentes à tester. On retourne le tour tel quel.

---

### Étape 2 — Extraction de l'ordre

```java
ArrayList<Integer> ordre = extraireOrdreIdsDepuisTour(tour);
int taille = ordre.size();
```
On travaille sur une liste d'identifiants `[id1, id2, id3, ..., idN]` plutôt que sur les objets `Noeud` ou les arcs directement.

Exemple avec 4 villes :
```
ordre = [1, 3, 2, 4]
```
Cela signifie : ville 1 → ville 3 → ville 2 → ville 4 → retour ville 1.

```java
Map<Integer, Noeud> noeudsParId = indexNoeudsParId();
```
Un index `id → Noeud` pour retrouver les coordonnées rapidement sans parcourir toute la liste à chaque fois.

---

### Étape 3 — Boucle principale

```java
boolean amelioration = true;
int passe = 0;

while (amelioration && passe < MAX_2OPT_PASSES) {
    amelioration = false;
    passe++;
    ...
}
```
On répète les passes jusqu'à ne plus trouver d'amélioration. La limite `MAX_2OPT_PASSES = 30` empêche une boucle trop longue sur les grands graphes.

A chaque nouvelle passe :
- on remet `amelioration = false`
- si on trouve une amélioration dans la passe, on met `amelioration = true` et on recommence

---

### Étape 4 — Double boucle : chercher deux arcs à échanger

```java
for (int i = 1; i < taille - 1; i++) {
    for (int j = i + 1; j < taille; j++) {
```
On parcourt toutes les paires d'arcs possibles. `i` et `j` sont les indices de début des deux arcs dans la liste `ordre`.

**Premier arc :** relie `ordre[i-1]` à `ordre[i]`  
**Deuxième arc :** relie `ordre[j]` à `ordre[j+1]`

---

### Étape 5 — Exclusions

```java
if (j == i + 1) {
    continue;
}
```
Si les deux arcs sont voisins (partagent un noeud), l'échange ne change rien. On saute.

```java
if (i == 1 && j == taille - 1) {
    continue;
}
```
Cas dégénéré : cela inverserait presque tout le cycle et reviendrait au même tour (juste parcouru dans l'autre sens). Aucun gain.

---

### Étape 6 — Les 4 noeuds clés

```java
int a = ordre.get(i - 1);   // noeud avant le premier arc
int b = ordre.get(i);        // noeud après le premier arc
int c = ordre.get(j);        // noeud avant le deuxième arc
int d = ordre.get((j + 1) % taille);  // noeud après le deuxième arc
```

Visualisation dans le tour :

```
... → a → b → ... → c → d → ...
          [arc 1]       [arc 2]
```

On veut savoir si remplacer `a-b` et `c-d` par `a-c` et `b-d` est moins coûteux.

Le `% taille` sur `j+1` gère le cas où `j` est le dernier indice (le cycle boucle).

---

### Étape 7 — Comparaison des coûts

```java
double ancienCout = distanceEntreIds(a, b, noeudsParId) + distanceEntreIds(c, d, noeudsParId);
double nouveauCout = distanceEntreIds(a, c, noeudsParId) + distanceEntreIds(b, d, noeudsParId);
```

- **ancienCout** : longueur des deux arcs actuels (a-b et c-d)
- **nouveauCout** : longueur des deux arcs après échange (a-c et b-d)

```java
if (nouveauCout + 1e-9 < ancienCout) {
```
Le `+ 1e-9` est une tolérance pour les erreurs d'arrondi des nombres flottants. Sans ça, on pourrait boucler sur des "améliorations" de 0.000000001 qui ne font rien en pratique.

---

### Étape 8 — Inversion de la sous-séquence

```java
inverserSousSequence(ordre, i, j);
amelioration = true;
ameliorationLocale = true;
break;
```

Quand on échange les arcs `a-b` et `c-d` par `a-c` et `b-d`, toute la portion du tour entre `b` et `c` doit être **parcourue dans le sens inverse**.

Avant :
```
ordre = [..., a, b, x1, x2, ..., c, d, ...]
```
Après inversion entre i et j :
```
ordre = [..., a, c, ..., x2, x1, b, d, ...]
```

La méthode `inverserSousSequence` fait simplement un swap des éléments entre les indices `i` et `j` :
```java
private void inverserSousSequence(ArrayList<Integer> ordre, int debut, int fin) {
    while (debut < fin) {
        int tmp = ordre.get(debut);
        ordre.set(debut, ordre.get(fin));
        ordre.set(fin, tmp);
        debut++;
        fin--;
    }
}
```
C'est un retournement en place, comme retourner un tableau.

Dès qu'une amélioration est trouvée, on sort des deux boucles et on recommence une passe depuis le début.

---

### Étape 9 — Reconstruction du tour

```java
ArrayList<Noeud> chemin = new ArrayList<>();
for (int id : ordre) {
    Noeud source = noeudsParId.get(id);
    chemin.add(source);
}

fermerCycle(chemin);
return construireTourDepuisChemin(chemin);
```

Une fois qu'aucune amélioration n'est possible, on reconstruit un `Graphe` propre depuis la liste `ordre` (maintenant dans l'ordre optimal trouvé) :
1. On recrée la liste de `Noeud` dans l'ordre
2. `fermerCycle` rajoute le noeud de départ à la fin pour fermer le circuit
3. `construireTourDepuisChemin` crée les arcs entre les noeuds consécutifs

---

## Exemple pas à pas — 4 villes

Villes : 1 (0,0), 2 (1,3), 3 (3,1), 4 (4,4)

**Tour initial** (produit par glouton par exemple) :
```
1 → 2 → 3 → 4 → 1
```

Coût total ≈ distance(1,2) + distance(2,3) + distance(3,4) + distance(4,1)

**Test de l'échange i=1, j=3** (arcs 1-2 et 3-4) :
- ancienCout = distance(1,2) + distance(3,4)
- nouveauCout = distance(1,3) + distance(2,4)

Si `nouveauCout < ancienCout` : on inverse la sous-séquence entre 1 et 3.

**Tour amélioré** :
```
1 → 3 → 2 → 4 → 1
```

On continue les passes jusqu'à ce qu'aucun échange ne soit bénéfique.

---

## Complexité

| Aspect | Valeur |
|---|---|
| Par passe | O(n²) — double boucle sur les paires d'arcs |
| Nombre de passes | au plus MAX_2OPT_PASSES = 30 |
| Total dans le pire cas | O(30 × n²) = O(n²) |
| Désactivé automatiquement si | n > 200 noeuds (dans le benchmark) |

La complexité O(n²) par passe vient du fait qu'on teste toutes les paires `(i, j)` possibles. Avec 100 villes, ça fait environ 5000 paires à tester à chaque passe.

---

## Garantie de qualité

Le 2-opt ne garantit **pas** l'optimal global. Il garantit seulement un **optimum local 2-opt** : aucun échange de deux arcs ne peut plus améliorer le tour.

En pratique, combiné avec glouton ou mstApprox, il donne des tours très proches de l'optimal sur les instances courantes.

---

## Résumé

| Étape | Ce qui se passe |
|---|---|
| 1 | Cas limites vérifiés |
| 2 | Extraction de l'ordre des villes |
| 3 | Boucle principale (max 30 passes) |
| 4 | Double boucle sur toutes les paires d'arcs |
| 5 | Identification des 4 noeuds a, b, c, d |
| 6 | Comparaison ancienCout vs nouveauCout |
| 7 | Si gain : inversion de la sous-séquence |
| 8 | Si aucun gain dans toute la passe : arrêt |
| 9 | Reconstruction du graphe final |
