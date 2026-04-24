# Explication détaillée — `glouton()`

## Vue d'ensemble

`glouton` implémente l'algorithme du **plus proche voisin** pour résoudre le problème du voyageur de commerce (TSP).

**Idée** : à chaque étape, on se déplace vers la ville non visitée la plus proche. C'est rapide mais pas optimal — le tour produit peut être jusqu'à 25% plus long que l'optimal.

---

## Pré-requis

Avant d'appeler `glouton()`, le graphe doit avoir été chargé (`importer()`) et rempli d'arcs (`complet()` ou `partiel()`).

- Si `complet()` : chaque nœud est relié à tous les autres → le fallback ne sera jamais déclenché.
- Si `partiel(k)` : chaque nœud n'est relié qu'à ses k plus proches voisins → le fallback peut être déclenché si le glouton se retrouve bloqué.

---

## Code annoté ligne par ligne

```java
public Graphe glouton() {
```
Retourne un nouveau `Graphe` représentant le **tour hamiltonien** trouvé (chaque ville visitée exactement une fois, circuit fermé).

---

### Étape 1 — Cas limite

```java
if (noeuds.isEmpty()) {
    return null;
}
```
Pas de nœuds = pas de tour possible.

---

### Étape 2 — Structures de travail

```java
ArrayList<Noeud> chemin = new ArrayList<>();
ArrayList<Noeud> nonVisites = new ArrayList<>(noeuds);
Set<Noeud> nonVisitesSet = new HashSet<>(nonVisites);
```

- `chemin` : l'ordre de visite construit au fur et à mesure.
- `nonVisites` : liste des villes restantes à visiter. Utilisée pour itérer en mode fallback.
- `nonVisitesSet` : **même contenu** que `nonVisites`, mais en HashSet.

**Pourquoi deux structures pour la même chose ?**

Vérifier si un nœud est dans une `ArrayList` = O(n) — on doit parcourir toute la liste.
Vérifier si un nœud est dans un `HashSet` = O(1) — accès direct par hash.

Dans la boucle principale, `nonVisitesSet.contains(voisin)` est appelé à chaque arc de chaque nœud. Sur un graphe complet de 52 villes ça représente des milliers d'appels → le HashSet est indispensable pour les performances.

---

### Étape 3 — Départ aléatoire

```java
int index = (int) (Math.random() * nonVisites.size());
Noeud courant = nonVisites.get(index);
chemin.add(courant);
nonVisites.remove(courant);
nonVisitesSet.remove(courant);
```

On choisit un nœud de départ **au hasard**. Pourquoi ?

L'algorithme glouton est sensible au point de départ — changer la ville de départ peut donner un tour plus court ou plus long. En lançant `glouton()` plusieurs fois (dans `evaluerComplexite()`), on obtient des résultats différents et on peut garder le meilleur.

---

### Étape 4 — Boucle principale : plus proche voisin

```java
while (!nonVisites.isEmpty()) {
    Noeud plusProche = null;
    double minDistance = Double.MAX_VALUE;
```

On tourne jusqu'à avoir ajouté toutes les villes au chemin.

---

#### Cas normal — le graphe a des arcs vers des voisins non visités

```java
for (Arc arc : courant.getArcs()) {
    Noeud voisin = arc.getN1().getId() == courant.getId() ? arc.getN2() : arc.getN1();

    if (nonVisitesSet.contains(voisin)) {
        double dist = arc.getValeur();
        if (dist < minDistance) {
            minDistance = dist;
            plusProche = voisin;
        }
    }
}
```

On parcourt **uniquement les arcs existants** dans le graphe source.

- `arc.getN1().getId() == courant.getId() ? arc.getN2() : arc.getN1()` : un arc relie deux nœuds. On récupère l'**autre** extrémité (le voisin), pas le nœud courant lui-même.
- `arc.getValeur()` : la distance est **déjà calculée et stockée** dans l'arc lors de `addArc()`. On ne recalcule rien ici → efficace.
- On garde le voisin avec la plus petite distance.

---

#### Cas fallback — graphe partiel bloqué

```java
if (plusProche == null) {
    for (Noeud n : nonVisites) {
        double dist = courant.distanceTo(n, this.isGeo);
        if (dist < minDistance) {
            minDistance = dist;
            plusProche = n;
        }
    }

    if (plusProche == null) {
        return null;
    }
}
```

Si `plusProche == null`, ça veut dire que **tous les voisins directs du nœud courant ont déjà été visités** (situation possible uniquement sur un graphe partiel).

Dans ce cas, on calcule la distance géométrique directe vers **tous** les nœuds non visités (sans passer par les arcs) et on prend le plus proche. C'est un plan B qui garantit qu'on ne reste pas bloqué.

---

### Étape 5 — Avancer vers le plus proche voisin

```java
chemin.add(plusProche);
nonVisites.remove(plusProche);
nonVisitesSet.remove(plusProche);
courant = plusProche;
```

On ajoute le voisin choisi au chemin, on le retire des deux structures "non visités", et il devient le nœud courant pour la prochaine itération.

---

### Étape 6 — Fermeture du cycle

```java
fermerCycle(chemin);
```

```java
// dans fermerCycle() :
chemin.add(chemin.get(0));
```

Ajoute le nœud de **départ** à la fin de la liste → le circuit est fermé.

Exemple : `[A, C, B, D]` devient `[A, C, B, D, A]`.

---

### Étape 7 — Construction du graphe résultat

```java
return construireTourDepuisChemin(chemin);
```

```java
// dans construireTourDepuisChemin() :
// 1. crée un nouveau Graphe vide
// 2. clone chaque Noeud du chemin (nouveaux objets, mêmes id/coords)
// 3. relie les noeuds consécutifs avec addArc() (recalcule et stocke les distances)
// 4. retourne ce graphe
```

On ne modifie **pas** le graphe source. On retourne un graphe séparé qui représente uniquement les arcs du tour trouvé. C'est ce graphe qu'on peut passer à `deuxOpt()` pour améliorer le résultat.

---

## Résumé visuel

```
importer() → complet() ou partiel()
                    ↓
            glouton() appelé
                    ↓
        départ aléatoire (ex: ville C)
                    ↓
    cherche le voisin non visité le plus proche
    via les arcs existants (ou fallback direct)
                    ↓
        se déplace → marque comme visité
                    ↓
        répète jusqu'à toutes les villes visitées
                    ↓
        ferme le cycle (retour au départ)
                    ↓
    construit un nouveau Graphe avec les arcs du tour
                    ↓
        [optionnel] deuxOpt() pour améliorer
```

---

## Dépendances de `glouton()`

| Méthode / structure utilisée | Rôle |
|---|---|
| `nonVisitesSet` (HashSet) | Vérification O(1) si un nœud est déjà visité |
| `arc.getValeur()` | Distance pré-calculée dans l'arc, pas de recalcul |
| `distanceTo(n, isGeo)` | Fallback uniquement si le graphe partiel bloque |
| `fermerCycle(chemin)` | Ajoute le nœud de départ en fin de liste |
| `construireTourDepuisChemin(chemin)` | Construit le Graphe résultat depuis la liste ordonnée |

---

## Complexité

| Cas | Complexité |
|---|---|
| Graphe complet | O(n²) — pour chaque nœud, on parcourt ses n-1 arcs |
| Graphe partiel (k voisins) | O(n·k) en cas normal, O(n²) si fallback déclenché |

Sur 52 villes avec graphe complet : 52 × 51 / 2 = 1326 arcs → très rapide en pratique.
