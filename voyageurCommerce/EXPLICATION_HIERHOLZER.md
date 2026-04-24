# Explication détaillée — `hierholzer()`

## Vue d'ensemble

`hierholzer()` trouve un **circuit eulérien** dans un multigraphe — c'est-à-dire un chemin qui passe par **chaque arc exactement une fois** et revient au point de départ.

**Précondition absolue** : tous les nœuds du graphe doivent avoir un **degré pair** (nombre d'arcs pair). C'est la condition nécessaire et suffisante pour qu'un circuit eulérien existe. C'est `christofides()` qui garantit cette condition avant d'appeler cette méthode.

**Utilisation** : `hierholzer()` est une méthode **privée**, utilisée uniquement par `christofides()`. Elle n'est jamais appelée directement.

---

## Notion clé — Circuit eulérien vs hamiltonien

| | Circuit eulérien | Circuit hamiltonien |
|---|---|---|
| Contrainte | Passer par chaque **arc** une fois | Passer par chaque **nœud** une fois |
| Ce qu'on cherche ici | ✓ | ✗ (c'est le but final du TSP) |

Hierholzer trouve un eulérien. Ensuite `christofides()` transforme ce circuit en hamiltonien par shortcutting (suppression des doublons de nœuds).

---

## Pré-requis — Pourquoi un multigraphe ?

Dans `christofides()`, on fusionne le MST et le matching minimum en un seul graphe. Ce graphe peut avoir **deux arcs entre les mêmes deux nœuds** (un venant du MST, un du matching) → c'est un **multigraphe**.

`hierholzer()` gère ça proprement via un **index numérique** par arc (`indexArc`) plutôt qu'une clé de type "id1-id2" qui ne pourrait pas distinguer deux arcs identiques.

---

## Code annoté ligne par ligne

```java
private ArrayList<Noeud> hierholzer(Map<Integer, Noeud> noeudsParId, ArrayList<Arc> arcs) {
```

**Paramètres** :
- `noeudsParId` : index `id → Noeud` pour retrouver un nœud par son ID.
- `arcs` : tous les arcs du multigraphe (MST + matching).

**Retour** : liste ordonnée de nœuds formant le circuit eulérien.

---

### Étape 1 — Construire la liste d'adjacence

```java
Map<Integer, ArrayList<int[]>> adj = new HashMap<>();
for (int i = 0; i < arcs.size(); i++) {
    Arc a = arcs.get(i);
    int id1 = a.getN1().getId();
    int id2 = a.getN2().getId();
    adj.computeIfAbsent(id1, x -> new ArrayList<>()).add(new int[] { id2, i });
    adj.computeIfAbsent(id2, x -> new ArrayList<>()).add(new int[] { id1, i });
}
```

On construit une **liste d'adjacence** : pour chaque nœud (par son ID), on stocke la liste de ses voisins sous forme de tableaux `[idVoisin, indexArc]`.

**Pourquoi `[idVoisin, indexArc]` et pas juste `idVoisin` ?**

Car c'est un multigraphe — deux nœuds peuvent être reliés par plusieurs arcs. On doit savoir **quel arc précis** on emprunte pour pouvoir le marquer comme utilisé et ne pas le reprendre.

Chaque arc non orienté apparaît **deux fois** dans `adj` (une entrée pour chaque extrémité), ce qui est correct : on peut l'emprunter dans les deux sens, mais on ne peut l'emprunter qu'**une seule fois** au total (géré par `arcUtilise[]`).

Exemple pour un arc A-B d'index 3 :
```
adj[A] contient : [..., [B, 3], ...]
adj[B] contient : [..., [A, 3], ...]
```

---

### Étape 2 — Tableau des arcs utilisés

```java
boolean[] arcUtilise = new boolean[arcs.size()];
```

Un tableau de booléens indexé par numéro d'arc. `arcUtilise[i] = true` signifie que l'arc d'index `i` a déjà été emprunté. C'est la seule façon de garantir que chaque arc est utilisé exactement une fois dans un multigraphe.

---

### Étape 3 — Pointeurs de position par nœud

```java
Map<Integer, Integer> ptr = new HashMap<>();
for (int id : adj.keySet()) {
    ptr.put(id, 0);
}
```

`ptr[v]` indique à quel **index** dans la liste de voisins de `v` on en est.

**Pourquoi ?** Sans ça, à chaque fois qu'on revient sur un nœud, on re-scannerait ses voisins depuis le début pour trouver le premier arc non utilisé — O(degré) à chaque visite. Avec le pointeur, on reprend exactement là où on s'était arrêté → O(1) amorti sur l'ensemble.

C'est ce qui rend Hierholzer efficace : on ne re-parcourt jamais un arc déjà utilisé.

---

### Étape 4 — Initialiser la pile et choisir le départ

```java
int depart = noeudsParId.keySet().iterator().next();

Deque<Integer> pile = new ArrayDeque<>();
ArrayList<Noeud> circuit = new ArrayList<>();
pile.push(depart);
```

Le départ peut être **n'importe quel nœud** — sur un graphe eulérien connexe, le circuit complet existe depuis n'importe quel point de départ.

`pile` est la pile de travail de Hierholzer (version itérative pour éviter les débordements de pile sur de grands graphes).

`circuit` accumule les nœuds dans l'ordre final du circuit eulérien.

---

### Étape 5 — Boucle principale de Hierholzer

```java
while (!pile.isEmpty()) {
    int v = pile.peek();
    ArrayList<int[]> voisins = adj.getOrDefault(v, new ArrayList<>());
    int p = ptr.getOrDefault(v, 0);
```

`pile.peek()` lit le sommet sans le retirer. `v` est le nœud courant.

---

```java
    while (p < voisins.size() && arcUtilise[voisins.get(p)[1]]) {
        p++;
    }
    ptr.put(v, p);
```

On avance le pointeur `p` jusqu'au **prochain arc non utilisé** de `v`. Les arcs déjà empruntés sont sautés. On met à jour `ptr[v]` pour s'en souvenir la prochaine fois.

---

#### Cas A — Plus d'arc disponible depuis v

```java
    if (p == voisins.size()) {
        circuit.add(noeudsParId.get(pile.pop()));
    }
```

`v` n'a plus d'arc disponible → il est "terminé". On le **retire de la pile** et on l'**ajoute au circuit**.

C'est le cœur de Hierholzer : les nœuds s'ajoutent au circuit en remontant la pile, ce qui produit le circuit dans le bon ordre.

---

#### Cas B — Il reste un arc disponible depuis v

```java
    } else {
        int[] suivant = voisins.get(p);
        arcUtilise[suivant[1]] = true;
        ptr.put(v, p + 1);
        pile.push(suivant[0]);
    }
```

- `suivant[0]` : ID du voisin à visiter.
- `suivant[1]` : index de l'arc emprunté → on le marque `true` pour ne plus le réutiliser.
- `ptr.put(v, p + 1)` : avance le pointeur pour la prochaine visite de `v`.
- `pile.push(suivant[0])` : on pousse le voisin sur la pile → on continue d'avancer.

---

## Exemple pas à pas (graphe simple)

Graphe triangulaire : A-B (arc 0), B-C (arc 1), C-A (arc 2). Départ : A.

| Étape | Pile | Action | Circuit |
|---|---|---|---|
| 1 | [A] | Arc A-B dispo → emprunter arc 0, pousser B | [] |
| 2 | [A, B] | Arc B-C dispo → emprunter arc 1, pousher C | [] |
| 3 | [A, B, C] | Arc C-A dispo → emprunter arc 2, pousher A | [] |
| 4 | [A, B, C, A] | Plus d'arc depuis A → pop A, ajouter au circuit | [A] |
| 5 | [A, B, C] | Plus d'arc depuis C (arc 2 utilisé) → pop C | [A, C] |
| 6 | [A, B] | Plus d'arc depuis B (arc 1 utilisé) → pop B | [A, C, B] |
| 7 | [A] | Plus d'arc depuis A (arc 0 utilisé) → pop A | [A, C, B, A] |
| 8 | [] | Pile vide → terminé | [A, C, B, A] |

Circuit eulérien : **A → C → B → A** (lu en inversant l'ordre d'insertion).

---

## Résumé visuel

```
[multigraphe MST + matching, tous degrés pairs]
                    ↓
     1. Construire liste d'adjacence [idVoisin, indexArc]
                    ↓
     2. Initialiser arcUtilise[] et pointeurs ptr[]
                    ↓
     3. Départ = premier nœud disponible
                    ↓
     4. Boucle :
        ├── Arc disponible depuis le sommet courant ?
        │     ├── OUI → marquer arc utilisé, pousser voisin
        │     └── NON → pop le sommet, l'ajouter au circuit
        └── Pile vide → fin
                    ↓
     retourner circuit (liste ordonnée de Noeuds)
```

---

## Dépendances de `hierholzer()`

| Structure utilisée | Rôle |
|---|---|
| `adj` (Map id → List[idVoisin, indexArc]) | Liste d'adjacence pour naviguer dans le multigraphe |
| `arcUtilise[]` (boolean[]) | Garantit que chaque arc est emprunté une seule fois |
| `ptr` (Map id → int) | Pointeur de position évitant de re-scanner les arcs utilisés |
| `pile` (Deque) | Pile de travail de l'algorithme (version itérative) |
| `noeudsParId` | Retrouver l'objet Noeud depuis un ID pour construire le circuit |

---

## Complexité

| Opération | Complexité |
|---|---|
| Construction liste d'adjacence | O(E) |
| Boucle principale | O(E) — chaque arc est emprunté exactement une fois |
| **Total** | **O(E)** |

C'est optimal : on ne peut pas faire mieux que parcourir tous les arcs au moins une fois.
