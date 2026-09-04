# Explication détaillée — `kruskal()`

## Vue d'ensemble

`kruskal()` construit un **Arbre Couvrant Minimal** (MST — Minimum Spanning Tree).

**Idée** : relier tous les nœuds avec le minimum de distance total, sans créer de cycle. Le résultat est un arbre (graphe connexe sans cycle) qui contient exactement **n-1 arcs** pour n nœuds.

**Utilisation** : `kruskal()` n'est pas un algorithme TSP direct. Il est utilisé comme **brique de base** pour `mstApprox()` et `christofides()`.

---

## Pré-requis

Le graphe doit avoir ses arcs calculés (`complet()` ou `partiel()`). Kruskal ne fonctionne qu'avec les arcs existants dans le graphe source.

---

## Notion clé — Union-Find

L'algorithme a besoin de savoir si deux nœuds sont **déjà dans le même groupe connecté** (pour éviter de créer un cycle). C'est le rôle de la structure **Union-Find** (aussi appelée Disjoint Set Union).

### Comment ça marche :

Au départ, chaque nœud est son propre groupe :
```
A → A    B → B    C → C    D → D
```

Quand on connecte A et B, on fusionne leurs groupes :
```
A → A    B → A    C → C    D → D
         (B appartient maintenant au groupe de A)
```

Si on veut ensuite connecter B et C, on cherche la **racine** de chaque nœud :
- racine de B = A
- racine de C = C
- Racines différentes → pas de cycle → on peut connecter.

Si on voulait connecter A et B à nouveau :
- racine de A = A
- racine de B = A (via B → A)
- **Même racine → cycle → on refuse l'arc.**

---

## Code annoté ligne par ligne

```java
public Graphe kruskal() {
```
Retourne un nouveau `Graphe` représentant le MST (arbre couvrant minimal).

---

### Étape 1 — Créer le graphe MST vide et cloner les nœuds

```java
Graphe mst = new Graphe();
mst.isGeo = this.isGeo;

Map<Integer, Noeud> copieNoeuds = new HashMap<>();
for (Noeud original : this.noeuds) {
    Noeud clone = new Noeud(original.getId(), original.getAbs(), original.getOrd());
    mst.getNoeuds().add(clone);
    copieNoeuds.put(clone.getId(), clone);
}
```

On crée de **nouveaux objets Noeud** (clones) pour le MST. Pourquoi ?

Si on réutilisait les mêmes objets, les arcs ajoutés au MST s'ajouteraient aussi aux nœuds du graphe source (via `n.getArcs().add(arc)` dans `addArc()`). Ça contaminerait le graphe original.

`copieNoeuds` est un index `id → Noeud cloné` pour retrouver rapidement le bon clone par son ID quand on ajoute un arc.

---

### Étape 2 — Trier les arcs par distance croissante

```java
ArrayList<Arc> arcsTries = new ArrayList<>(this.arcs);
arcsTries.sort((a, b) -> Double.compare(a.getValeur(), b.getValeur()));
```

On copie la liste des arcs et on la trie du plus court au plus long. C'est l'étape centrale de Kruskal : en traitant les arcs dans cet ordre, on est sûr d'ajouter toujours le plus court arc possible qui n'est pas encore couvert.

---

### Étape 3 — Initialiser l'Union-Find

```java
Map<Noeud, Noeud> parent = new HashMap<>();
Map<Noeud, Integer> rank = new HashMap<>();

for (Noeud n : this.noeuds) {
    parent.put(n, n);
    rank.put(n, 0);
}
```

- `parent` : à chaque nœud associe son **parent** dans l'arbre Union-Find. Au départ chaque nœud est son propre parent (il est la racine de son propre groupe).
- `rank` : hauteur approximative de l'arbre Union-Find pour chaque racine. Sert à optimiser les fusions (voir `union()` plus bas). Au départ tout est à 0.

---

### Étape 4 — Parcourir les arcs triés

```java
for (Arc a : arcsTries) {

    Noeud r1 = find(parent, a.getN1());
    Noeud r2 = find(parent, a.getN2());

    if (r1 != r2) {

        Noeud n1Mst = copieNoeuds.get(a.getN1().getId());
        Noeud n2Mst = copieNoeuds.get(a.getN2().getId());
        mst.addArc(n1Mst, n2Mst);

        union(parent, rank, r1, r2);

        if (mst.getArcs().size() == this.noeuds.size() - 1) {
            break;
        }
    }
}
```

Pour chaque arc (du plus court au plus long) :

1. **`find(parent, a.getN1())`** : trouve la racine du groupe du premier nœud (voir détail ci-dessous).
2. **`find(parent, a.getN2())`** : idem pour le second.
3. **`if (r1 != r2)`** : si les racines sont différentes, les deux nœuds sont dans des groupes séparés → pas de cycle → on peut ajouter l'arc.
4. On ajoute l'arc avec les **clones** du MST (via `copieNoeuds`), pas les nœuds originaux.
5. **`union(parent, rank, r1, r2)`** : fusionne les deux groupes en un seul.
6. **Condition d'arrêt** : un MST sur n nœuds a exactement **n-1 arcs**. Dès qu'on l'atteint, on arrête — inutile de parcourir le reste.

---

## Méthodes auxiliaires

### `find()` — Trouver la racine d'un nœud

```java
private Noeud find(Map<Noeud, Noeud> parent, Noeud n) {
    Noeud p = parent.get(n);
    if (p != n) {
        parent.put(n, find(parent, p));
    }
    return parent.get(n);
}
```

Remonte la chaîne de parents jusqu'à trouver la racine (le nœud qui est son propre parent).

**Compression de chemin** : `parent.put(n, find(parent, p))` — après avoir trouvé la racine, on fait pointer `n` **directement** sur la racine (court-circuit). La prochaine fois qu'on cherchera la racine de `n`, ce sera O(1).

Exemple avant compression :
```
D → C → B → A (racine)
```
Après `find(D)` avec compression :
```
D → A    C → A    B → A    A → A
```

---

### `union()` — Fusionner deux groupes

```java
private void union(Map<Noeud, Noeud> parent, Map<Noeud, Integer> rank, Noeud a, Noeud b) {
    Noeud racineA = find(parent, a);
    Noeud racineB = find(parent, b);

    if (racineA == racineB) {
        return;
    }

    int rankA = rank.get(racineA);
    int rankB = rank.get(racineB);

    if (rankA < rankB) {
        parent.put(racineA, racineB);
    } else if (rankA > rankB) {
        parent.put(racineB, racineA);
    } else {
        parent.put(racineB, racineA);
        rank.put(racineA, rankA + 1);
    }
}
```

Fusionne les deux groupes en attachant la racine du plus **petit** arbre sous la racine du plus **grand** (union par rang).

**Pourquoi ?** Pour éviter que l'arbre Union-Find devienne une longue chaîne (dégénérescence). En attachant toujours le petit sous le grand, l'arbre reste peu profond → `find()` reste rapide.

- `rankA < rankB` → A devient fils de B (arbre B est plus haut, il "absorbe" A)
- `rankA > rankB` → B devient fils de A
- `rankA == rankB` → peu importe lequel absorbe l'autre, mais le rang de la racine choisie augmente de 1

---

## Résumé visuel

```
[graphe complet ou partiel avec arcs calculés]
                    ↓
        kruskal() appelé
                    ↓
    1. Cloner tous les nœuds → MST vide
                    ↓
    2. Trier tous les arcs par distance croissante
                    ↓
    3. Initialiser Union-Find (chaque nœud = son propre groupe)
                    ↓
    4. Pour chaque arc (du plus court au plus long) :
       ├── find() les racines des deux nœuds
       ├── Racines différentes ?
       │     ├── OUI → ajouter l'arc au MST + union() des groupes
       │     └── NON → ignorer (créerait un cycle)
       └── MST a n-1 arcs ? → STOP
                    ↓
        retourner le MST
```

---

## Exemple concret (4 villes)

Arcs triés : A-B(1), C-D(2), A-C(3), B-D(5), A-D(6), B-C(7)

| Arc traité | Racines | Cycle ? | Action |
|---|---|---|---|
| A-B (dist 1) | A ≠ B | Non | Ajouter → union(A,B) |
| C-D (dist 2) | C ≠ D | Non | Ajouter → union(C,D) |
| A-C (dist 3) | A ≠ C | Non | Ajouter → union(A,C) — **3 arcs = n-1 → STOP** |
| B-D (dist 5) | — | — | Jamais traité |

MST final : A-B, C-D, A-C avec coût total = 1+2+3 = 6.

---

## Dépendances de `kruskal()`

| Méthode / structure utilisée | Rôle |
|---|---|
| `find(parent, n)` | Trouve la racine d'un nœud avec compression de chemin |
| `union(parent, rank, r1, r2)` | Fusionne deux groupes sans dégénérer l'arbre |
| `copieNoeuds` (HashMap id→Noeud) | Retrouve le clone du bon nœud lors de l'ajout d'arcs |
| `addArc(n1, n2)` | Ajoute l'arc au MST et stocke la distance |

---

## Complexité

| Opération | Complexité |
|---|---|
| Tri des arcs | O(E log E) — E = nombre d'arcs |
| Chaque `find()` + `union()` | O(α(n)) ≈ O(1) amorti (α = inverse d'Ackermann, quasi-constant) |
| **Total** | **O(E log E)** — dominé par le tri |

Sur un graphe complet de 52 villes : E = 52×51/2 = 1326 arcs → très rapide.
