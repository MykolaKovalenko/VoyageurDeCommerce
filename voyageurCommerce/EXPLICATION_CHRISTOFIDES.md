# Explication detaillee - `christofides()`

## Vue d'ensemble

`christofides()` est une methode d'approximation du TSP (voyageur de commerce) avec garantie theorique forte.

Pipeline general :
1. Construire un MST avec Kruskal.
2. Trouver les noeuds de degre impair du MST.
3. Calculer un minimum perfect matching sur ces noeuds impairs.
4. Fusionner MST + matching en multigraphe eulerien (tous degres pairs).
5. Extraire un circuit eulerien (Hierholzer).
6. Faire du shortcut (supprimer repetitions de noeuds) pour obtenir un tour hamiltonien.

Garantie theorique (metrique avec inegalite triangulaire) :

- Le cout du tour trouve par Christofides est au maximum 1,5 fois le meilleur cout possible.
- En clair : si la meilleure solution possible vaut 100, Christofides ne depassera pas 150.
- Ici, `C*` veut dire "cout optimal" (la meilleure solution theorique).

Dans votre code, les distances GEO/euclidiennes respectent l'idee metrique utilisee par cette borne.

---

## Signature et cas limite

```java
public Graphe christofides() {

    if (noeuds.isEmpty()) {
        return null;
    }
```

- Retourne un nouveau `Graphe` representant le tour final.
- Si le graphe source n'a aucun noeud, on renvoie `null`.

---

## Etape 1 - MST

```java
Graphe mst = this.kruskal();
```

On construit l'arbre couvrant minimal du graphe courant.

Pourquoi : le MST donne une structure de base a faible cout, sans cycle.

---

## Etape 2 - Minimum matching sur degres impairs

```java
Graphe mm = this.minimumMatching(mst);
```

`minimumMatching(mst)` fait en interne :
1. Extraire les noeuds de degre impair du MST.
2. Construire les distances entre ces noeuds.
3. Trouver le matching parfait minimum (dans votre code : DP exacte par bitmask, avec limite de taille).

Pourquoi : dans tout graphe, le nombre de noeuds impairs est pair. En les appariant, on ajoute des arcs qui rendent ces degres pairs.

---

## Etape 3 - Construire le multigraphe MST + MM

```java
Map<Integer, Noeud> noeudsParId = new HashMap<>();
for (Noeud n : this.noeuds) {
    noeudsParId.put(n.getId(), n);
}
```

On cree un index ID -> Noeud du graphe source pour retrouver les bons objets (coords correctes).

Ensuite, on accumule tous les arcs de MST et de MM dans une meme liste :

```java
ArrayList<Arc> tousArcs = new ArrayList<>();
for (Arc a : mst.getArcs()) {
    Noeud n1 = noeudsParId.get(a.getN1().getId());
    Noeud n2 = noeudsParId.get(a.getN2().getId());
    tousArcs.add(new Arc(n1, n2, a.getValeur()));
}
for (Arc a : mm.getArcs()) {
    Noeud n1 = noeudsParId.get(a.getN1().getId());
    Noeud n2 = noeudsParId.get(a.getN2().getId());
    tousArcs.add(new Arc(n1, n2, a.getValeur()));
}
```

Important : on parle bien de multigraphe.
- Un arc peut apparaitre plusieurs fois entre deux memes IDs.
- C'est normal et attendu pour Hierholzer.

Cas limite :

```java
if (tousArcs.isEmpty()) {
    return null;
}
```

---

## Etape 4 - Circuit eulerien (Hierholzer)

```java
ArrayList<Noeud> circuit = hierholzer(noeudsParId, tousArcs);

if (circuit.isEmpty()) {
    return null;
}
```

Comme tous les degres du multigraphe sont pairs, un circuit eulerien existe (sous hypothese de connexite exploitable).

`hierholzer(...)` renvoie un ordre de noeuds qui parcourt chaque arc exactement une fois.

---

## Etape 5 - Shortcut pour passer d'eulerien a hamiltonien

```java
ArrayList<Noeud> chemin = new ArrayList<>();
Set<Integer> dejaVus = new HashSet<>();
for (Noeud n : circuit) {
    if (dejaVus.add(n.getId())) {
        chemin.add(n);
    }
}
```

On garde uniquement la premiere apparition de chaque noeud (par ID).

Pourquoi c'est valide :
- Le circuit eulerien peut revisiter des noeuds.
- En TSP on veut visiter chaque noeud une seule fois.
- Avec l'inegalite triangulaire, remplacer un detour par un saut direct n'augmente pas le cout.

---

## Etape 6 - Fermer le cycle et construire le graphe final

```java
fermerCycle(chemin);
return construireTourDepuisChemin(chemin);
```

- `fermerCycle` ajoute le noeud de depart en fin de liste.
- `construireTourDepuisChemin` clone les noeuds du chemin et cree les arcs consecutifs.

Resultat : un tour TSP complet pret a evaluer avec `cout()` ou ameliorer avec `deuxOpt()`.

---

## Schema complet du flux

```text
Graphe source
   |
   +--> kruskal() -> MST
   |
   +--> minimumMatching(MST) -> MM sur noeuds impairs
   |
   +--> fusion MST + MM -> multigraphe eulerien
   |
   +--> hierholzer(...) -> circuit eulerien
   |
   +--> shortcut (supprimer repetitions de noeuds)
   |
   +--> fermerCycle + construireTourDepuisChemin
   v
Tour hamiltonien approxime (Christofides)
```

---

## Ce que `christofides()` utilise exactement

| Methode / structure | Role |
|---|---|
| `kruskal()` | Construit le MST |
| `minimumMatching(mst)` | Apparier au cout minimal les noeuds impairs du MST |
| `Map<Integer, Noeud> noeudsParId` | Refaire le lien ID -> Noeud source |
| `ArrayList<Arc> tousArcs` | Construire le multigraphe MST + MM |
| `hierholzer(noeudsParId, tousArcs)` | Extraire le circuit eulerien |
| `Set<Integer> dejaVus` | Shortcut vers chemin hamiltonien |
| `fermerCycle(chemin)` | Revenir au noeud de depart |
| `construireTourDepuisChemin(chemin)` | Generer le graphe resultat final |

---

## Complexite (ordre de grandeur)

Notations :
- n = nombre de noeuds
- E = nombre d'arcs du graphe source
- m = nombre de noeuds impairs dans le MST

Couts dominants :
1. Kruskal : O(E log E). Lisible comme "nombre d'arcs" x "petit facteur de tri".
2. Minimum matching exact (DP bitmask) : O(m^2 2^m) en temps et O(2^m) en memoire. C'est la partie qui explose le plus vite.
3. Hierholzer : O(E') sur le multigraphe. Concretement, on passe en gros une fois sur les arcs fusionnes.
4. Shortcut + reconstruction : O(n). Un passage simple sur les noeuds/chemins.

Donc, dans votre implementation, le goulot peut devenir `minimumMatching()` quand m grandit, d'ou la limite defensive deja presente dans le code.

En version tres simple :
- Kruskal et Hierholzer passent bien a l'echelle.
- La vraie limite vient du minimum matching exact.
- C'est pour cela que votre code bloque volontairement au-dela d'un certain nombre de noeuds impairs.

---

## Exemple intuitif tres court

1. MST relie toutes les villes avec peu de distance.
2. Noeuds impairs du MST : par exemple 6 noeuds.
3. Matching minimum ajoute 3 arcs bien choisis.
4. Tous les degres deviennent pairs -> circuit eulerien possible.
5. Hierholzer donne un grand circuit qui repasse parfois par des villes.
6. On supprime les repetitions dans l'ordre de premiere apparition -> tour TSP final.

---

## Point important specifique a votre code

Votre `minimumMatching` est exact mais borne (max 22 noeuds impairs), donc :
- Excellent sur petites/moyennes instances.
- Peut lever une exception sur grosses instances si trop de noeuds impairs.

C'est un choix coherent pour garder des resultats de qualite sans exploser en temps/memoire.
