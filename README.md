# Portfolio-Performance-ETF-Import
additional tool to automatically import ETF data into classifications

**(PP = Portfolio Performance)**

english version missing;

Erste Entwicklung um Java kennen zu lernen; dient hauptsächlich zum Ausprobieren.
Ziel war es, eine Automatisierung der Klassifizierung aller ETF zu implementieren.
Sowohl Regionen, Branchen und eine eigene Klassifizierung (Top Ten) werden mithilfe dieses Programms in eine bestehende Portfolio Performance .xml Datei eingebunden. Die Daten dafür werden von onvista.de geladen.

Code befindet sich im "src" Ordner, Beispiele wie es dann später aussieht im "example" Ordner.


## Erklärung der Klassifikationen

#### Regionen
Hier werden die Anteile der ETF in die einzelnen Regionen verteilt.

#### Branchen (GICS)
Hier werden die ETF anteilig in die Branchen eingeteilt. Es gibt jedoch Abweichungen bei der Benamung seites onvista und GICS.
Das Programm versucht die passende Branche zu ermitteln, dies ist in den meisten Fällen auch korrekt.
Nachträglich lässt sich mithilfe der Textdateien im "ETF Details" Ordner nachverfolgen, wie die Einteilung stattgefunden hat.
In PP kann nachträglich in die entsprechende Branche verschoben werden.

#### Top Ten
Hier werden die Top 10 Aktien aller ETF eingebunden. Gibt es keinerlei Überschneidungen der ETF, ist diese Klassifikation nicht nötigt.
Dies ist jedoch selten der Fall, im Beispielbild wird der bekannte All World sowie NASDAQ verwendet. Man sieht deutlich, dass es eine Überschneidung gibt und so kann man nachvollziehen, wie hoch der Anteil der großen Einzeilpositionen pro ETF summiert über alle ETF im Portfolio ist.
Im Beispiel zeigt sich, wie hoch exakt der Anteil von Apple ist. (Dass Apple im All World und NASDAQ vertreten ist, ist wahrscheinlich jedem klar).
Aber zeigt auch, dass die größten 10 Positionen der beiden ETF rund 25% des Portfolios ausmacht.
Hinweis: Die Bezeichnung der Unternehmen variert pro ETF (z.B. Apple und Apple Inc. und APPLE). Hier muss nachträglich noch etwas in PP sortiert werden um die ETF Anteile in einem Ordner zusammen zuführen.



## How To

**Zunächst rate ich zu einer backup der .xml Datei (auch wenn die ursprüngliche Datei nicht verändert wird, sondern eine neue generiert wird).**

### Vorbereitung
Regionen sowie Branchen lassen sich in Portfolio Performance direkt bei "Klassifikationen" hinzufügen, für Top Ten muss eine neue angelegt und entsprechend benannt werden.
Den Inhalt von "runnable .jar" sowie die xml Datei in einen Ordner packen. Wichtig ist hierbei, dass die xml Datei "Portfolio Performance.xml" heißt. Hier muss die (eigene) Datei umbenannt werden!

### Programmstart
Im Windows Explorer lässt sich mithilfe von "Datei -> Windows Powershell öffnen" (oben links) eine Konsole öffnen.
Mittels "java -jar WaffelImport.jar" (Enter drücken) startet das Programm.
Beim ersten Start ist es normal, dass die Speicherungs Datei nicht vorhanden ist und alles importiert werden soll.
Diese Speicherungs Datei verhindert bei späteren Ausführungen, dass doppelte Einträge angelegt werden (jedoch wird nach einem Rebalancing des ETF ein neuer Eintrag erzeigt, wenn die Gewichtung einer Branche sich verändert hat).

### Verwendung
Im Anschluss kann die neu erzeugte Datei "Portfolio Performance Import.xml" in PP geladen werden und das Ergebnis überprüft werden.
Kleinere Anpassungen werden dennoch in Branchen und Top Ten nötig sein!
Man kann nun in den Klassifizierungen auf das Hauptelement rechtsklicken -> Farbe -> Farbe zufällig zuweisen; verbessert die Übersicht/lesbarkeit enorm.



Disclaimer:
Mir ist bewusst, dass es nicht unbedingt der sauberste code ist, jedoch würde mich als Softwareentwickler die Meinung anderer interessieren.
Von guiding lines über performance (da meine erste Erfahrung mit Java darauf wenig Wert gelegt).
