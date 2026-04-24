# Explication détaillée — `mstApprox()`

## Vue d'ensemble

`mstApprox()` est une **approximation TSP basée sur le MST**. Elle produit un tour hamiltonien (circuit passant par toutes les villes exactement une fois) en exploitant l'arbre couvrant minimal.

**Garantie théorique** : le tour produit ne dépasse jamais **2× l'optimal** (algorithme 2-approx). En pratique il est souvent bien meilleur.

**Pipeline en 5 étapes** :
```
kruskal() → DFS → supprimer doublons → fermer cycle → construire graphe
```

---

## Pré-requis

Le graphe doit avoir ses arcs calculés (`complet()` ou `partiel()`). `mstApprox()` appelle `kruskal()` en interne — pas besoin de l'appeler avant.

---

## Pourquoi ça marche ? (intuition)

Un MST relie tous les nœuds avec le minimum de distance. Si on le parcourt en DFS, on visite chaque nœud — mais on **repasse** par des nœuds déjà visités (car c'est un arbre, pas un cycle).

En **sautant les doublons** (shortcutting), on obtient un chemin hamiltonien. La propriété de l'inégalité triangulaire garantit que ces raccourcis ne coûtent jamais plus que le détour original → le coût ne peut qu'être inférieur ou égal.

---

## Code annoté ligne par ligne

```java
public Graphe mstApprox() {
```
Retourne un nouveau `Graphe` représentant le tour hamiltonien approximé.

---

### Étape 1 — Construire le MST

```java
Graphe mst = this.kruskal();

if (mst.getNoeuds().isEmpty()) {
    return mst;
}
```

On délègue complètement à `kruskal()`. Le MST obtenu contient tous les nœuds et exactement n-1 arcs, sans cycle.

Le graphe vide en entrée → graphe vide en sortie (cas limite).

---

### Étape 2 — DFS sur le MST

```java
ArrayList<Noeud> parcours = new ArrayList<>();
Set<Noeud> visite = new HashSet<>();

for (Noeud depart : mst.getNoeuds()) {
    if (!visite.contains(depart)) {
        mst.dfs(depart, visite, parcours);
    }
}
```

On effectue un **parcours en profondeur (DFS)** sur le MST pour obtenir un ordre de visite.

**Pourquoi une boucle autour du DFS ?**

Si le graphe source était partiel et mal connecté, le MST pourrait être composé de plusieurs composantes disconnectées. La boucle garantit qu'on démarre un DFS depuis chaque composante non encore visitée — aucun nœud n'est oublié.

En pratique sur un graphe complet, un seul DFS suffit (tout est connecté dans le MST).

---

### `dfs()` — Parcours en profondeur

```java
private void dfs(Noeud n, Set<Noeud> visite, ArrayList<Noeud> parcours) {
    visite.add(n);
    parcours.add(n);

    for (Arc a : n.getArcs()) {
        Noeud voisin = a.getN1().equals(n) ? a.getN2() : a.getN1();

        if (!visite.contains(voisin)) {
            dfs(voisin, visite, parcours);
        }
    }
}
```

Fonctionnement :

1. **Marquer** le nœud comme visité (dans `visite`) et l'ajouter au parcours.
2. **Pour chaque arc** du nœud courant, récupérer l'autre extrémité (le voisin).
3. **Si le voisin n'a pas encore été visité** → appel récursif (on descend en profondeur).
4. Quand tous les voisins sont traités → retour (remontée).

`visite` est un `HashSet` → `contains()` en O(1), indispensable car appelé à chaque arc.

**Ce que produit le DFS** : une liste où chaque nœud peut apparaître **plusieurs fois** (on repasse par un nœud en remontant l'arbre).

Exemple sur un MST :
```
     A
    / \
   B   C
  / \
 D   E
```
DFS depuis A : `[A, B, D, B, E, B, A, C, A]`
(les nœuds en gras sont les "re-passages" en remontant)

---

### Étape 3 — Supprimer les doublons → chemin hamiltonien

```java
ArrayList<Noeud> chemin = new ArrayList<>();
Set<Noeud> dejaAjoutes = new HashSet<>();

for (Noeud n : parcours) {
    if (dejaAjoutes.add(n)) {
        chemin.add(n);
    }
}
```

On parcourt le résultat du DFS et on ne garde que la **première apparition** de chaque nœud. `dejaAjoutes.add(n)` retourne `true` si le nœud n'était pas encore dans le Set (= première fois qu'on le voit), `false` sinon.

En reprenant l'exemple précédent :
```
Parcours DFS : [A, B, D, B, E, B, A, C, A]
Chemin final : [A, B, D, E, C]
              (B, A répétés → ignorés)
```

C'est le **shortcutting** : au lieu de A→B→D→**B**→E→**B**→**A**→C, on "saute" directement D→E→C. Grâce à l'inégalité triangulaire (distance directe ≤ détour), ce raccourci ne coûte pas plus cher.

---

### Étape 4 — Fermer le cycle

```java
fermerCycle(chemin);
```

```java
// dans fermerCycle() :
chemin.add(chemin.get(0));
```

Ajoute le nœud de départ à la fin → circuit fermé.

Exemple : `[A, B, D, E, C]` devient `[A, B, D, E, C, A]`.

---

### Étape 5 — Construire le graphe résultat

```java
return construireTourDepuisChemin(chemin);
```

Crée un nouveau `Graphe` vide, clone les nœuds du chemin, relie les nœuds consécutifs avec `addArc()` (qui recalcule et stocke les distances), et retourne ce graphe.

---

## Résumé visuel complet

```
[graphe avec arcs calculés]
            ↓
     1. kruskal()
        → MST : tous les nœuds reliés, n-1 arcs, sans cycle
            ↓
     2. DFS sur le MST
        → liste ordonnée avec doublons
          ex: [A, B, D, B, E, B, A, C, A]
            ↓
     3. Supprimer doublons (garder 1ère apparition)
        → chemin hamiltonien sans répétition
          ex: [A, B, D, E, C]
            ↓
     4. Fermer le cycle
        → [A, B, D, E, C, A]
            ↓
     5. construireTourDepuisChemin()
        → nouveau Graphe avec arcs A-B, B-D, D-E, E-C, C-A
            ↓
     [optionnel] deuxOpt() pour améliorer
```

---

## Comparaison avec `glouton()`

| | `glouton()` | `mstApprox()` |
|---|---|---|
| Stratégie | Plus proche voisin à chaque étape | MST + DFS + shortcut |
| Garantie | Aucune (peut être très mauvais) | ≤ 2× l'optimal |
| Départ | Aléatoire (résultats variables) | Déterministe (même résultat) |
| Dépendances | Arcs du graphe source | `kruskal()` + `dfs()` |
| Complexité | O(n²) | O(E log E) pour Kruskal + O(n) pour DFS |

---

## Dépendances de `mstApprox()`

| Méthode utilisée | Rôle |
|---|---|
| `kruskal()` | Construit le MST |
| `dfs(n, visite, parcours)` | Parcours en profondeur du MST |
| `fermerCycle(chemin)` | Ajoute le nœud de départ en fin |
| `construireTourDepuisChemin(chemin)` | Construit le Graphe résultat |

---

## Complexité

| Opération | Complexité |
|---|---|
| `kruskal()` | O(E log E) |
| `dfs()` | O(n + E_mst) = O(n) car MST a n-1 arcs |
| Suppression doublons | O(n) |
| **Total** | **O(E log E)** — dominé par Kruskal |

Sur un graphe complet de 52 villes : E = 1326 → très rapide.
