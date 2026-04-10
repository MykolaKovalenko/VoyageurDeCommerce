## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).

--------------------------------------------------------------------
PROBLÈME 1 — ton i++ (toujours le point critique)

👉 Oui, ça “marche” dans tes tests
👉 MAIS ce n’est pas fiable

Pourquoi concrètement
for (int i = 0; i < k; i++)

ET tu fais :

else {
    i++;
}

--------------------------------------------------------------------
PROBLÈME 2 — glouton() partage les objets Noeud
resultat.getNoeuds().addAll(chemin);

✔ Correction
for (Noeud n : chemin) {
    resultat.getNoeuds().add(new Noeud(n.getId(), n.getAbs(), n.getOrd()));
}
--------------------------------------------------------------------
importer()

Tu fais :

else
    return;

👉 si la première ligne ≠ GEO ou 2D → tu ignores tout le fichier
--------------------------------------------------------------------