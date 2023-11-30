 Aktoren

## A1: Der Website-Konsument
## A2: Der Website-Pfleger
## A3: Der Website-Entwickler
## A4: Der Website Betreiber

# Requirements - Website Redesign - Brainstorming

## 001
### Modularisierung von Templates
Als Website-Pfleger möchte ich Templates versioniert verwenden können. Damit kann ich entscheiden, wann ich Template Weiterentwicklungen folgen möchte.

Bei hugo:
wird typischerweise mit git submodule umgesetzt 
=> Micha meint, dass dann der Entwicklungsroundtrip um 1 bis 2 Schritte größer wird. 
(git pull im submodul ordner & keine sprechende versionsnumme).
Addendum: Es gibt hugo modules, die Versionsverwaltung und Modularisierung einfach machen: https://www.nickgracilla.com/posts/master-hugo-modules-managing-themes-as-modules/

bei cryogen:
modularisierung über jar-file => im kombinierten Entwicklungszyklus wird mit snapshot ein Schritt weniger gebraucht.

## 002
### Websitestruktur ist einfach zu lernen
Die fertige Websitestruktur soll möglichst unkompliziert und somit einfach für den Website-Pfleger zu lernen sein.

These: Damit jemand die Website-Struktur lernen kann, muss klar sein, wie das Theme funktionieert und wir wir unsere Inhalte strukturieren.

Theme: Setzt sich aus der Menge der Layouts, CSS und JS zusammen

Layout: Besteht aus einem oder mehreren Partials und repräsentiert einen Seitentyp im Theme

Partials: sind HTML und Template Elemente, die in Layouts enthalten sein können.

Asset: 

Hugo: Erlaubt Trennung von Inhalt und Theme, relativ viel Freiheit bei Folder-Structure (Erik sucht evtl. noch Details raus)

Cryogen: Hat Inhalte und Theme gemischt, Folder sind teilweise fest im Code verdrahtet

Die Doku muss auf Theme-Ebene gemacht werden.

## 003
### Responsive Design
Als Website-Konsument möchte ich die Website auf Smartphon, Tablet oder auf dem 4K-Sreen konsumieren können.

Große Auswahl an Themes bei Hugo, Auswahl bei reinen Bootstrap-themes (noch) unklar.
Zeit in Recherche von Themes investieren (2h) mit Fokus auf Aussehen, interne Struktur und Erweiterbarkeit.

## 004
### Inhalte ohne Layout
Der Website-Pfleger soll Inhalte möglichst einfach verändern können, ohne dass er sich um das Layout kümmern muss (evtl Markdown oder Asciidoc ?).

Michael und Ansgar wollen Seiten mit Markdown machen können.
Erik findet Markdown auf den LandingPages nicht wichtig und in manchen Fällen kontraproduktiv.

## 005
### Website muss keine Inhalte von extern downloaden (müssen) - z.B. Fonts, css etc
Als Website-Konsument möchte ich nur der besuchten Seite meine Daten zugestehen - und nicht der gesamten tracking-Welt.

Geht bei beiden.

## 007
### Rückwärtskompatibilität
Als Website-Betreiber möchte ich, dass alte Websites (bspw Informatikbüro Jerger) mit c4k-website weiterhin funktionieren, ohne bei ihnen Anpassungen machen zu müssen (wir brauchen eine Wartungsperspektive für 3 weitere bestehende Websites).

Todos sind:
* lein als build-aufruf muss einheitlicher werden
* c4k-website müsste evtl. mit einem weitern flavor "hugo" umgehen können
* Migrationspfad für bestehende Websites (Anleitung für Navigation / bisherige Content-Struktur)
* POC: der die wesentlichen Features für dda.io abbildet (Navigation, Migration für Seiten&Bilder, Blog, partials)

## 008
### Website soll statisch sein
Als Website-Betreiber möchte ich eine statische Website ausliefern, damit der Website Betrieb nicht so komplex wird. Der Website betreiber muss sich zudem bei statischen Websites nicht so sehr um die Sicherheit von Credentials, Assets und ähnliches kümmern.

Geht bei beiden.

## 009
### Einfache und Zugängliche Technologie
a) Die Technologie, die der Theme-Entwickler zum Bau der WS verwendet (mit Technik drumherum), soll gut dokumentiert und zugänglich sein. Damit ist es für den Website-Entwickler einfach, Änderungen im technischen Kontext der Website umzusetzen.

Beim Generator stehen da so Fragen an wie:
    * Lookup-Order in der Dir-Struktur
    * Modul-Frage
    * Content Organisation (Page-Bundles bei hugo)
    * Debug-Möglichkeiten
    
Community (Hugo) <-> Wissen in der Firma (Micha hat das Zeugs in Cryogen eingebaut)
(Ausführliche) Dokumentation

b) Die Technologie, die der Content-Entwickler verwendet, soll gut dokumentiert und zugänglich sein. Damit ist es für den Website-Entwickler einfach, Änderungen im Inhalt umzusetzen.

Beim Theme stehen da so Fragen an wie:
    * Wie stelle ich navigation her
    * Link in texts
    * Welche Layouts existieren
    * Bilder
    * Meta-Infos
    * Muss ich Graph Head bedienen?

## 010
### Spass bei Entwicklung und Pflege
Website-Entwickler und Website-Pfleger sollten Spaß haben bei Website redesignen und Pflege (techn. und inhaltliche Pflege)

Spassfaktoren:
    * Startgeschwindigkeit des Tools
* Hugo startet schnell, ohne JDK
* Cryogen braucht länger, kann evtl mit GraalVM beschleunigt werden
* Buildzeit
* Hugo kommt and WYSIWYG feeling heran, buildzeit zur Vorschau bei <1s
* Cryogen builds brauchen ca +-10s
* Entwicklungsflow
* Instantane Anzeige von Änderungen auf der Site bei Hugo
* Editor-Freiheit
* Markdown hat schon im Editor ein PreView
* Markdown erlaubt kleine Freiheiten, ohne immer gleich am Theme ändern zu müssen.
* Bild-Nachbearbeitungspipeline
* Mehr Automatisierung bei Bild-Metainfos, Sizing, Dimensionierung
* Spassbremse: Hugo erinnert mich an Helm - und das hat Brechreitzfaktor für Micha
     * Hugo wäre Investierung in die Zukunft
     * Hugo-Wissen kann auf dem Lebenslauf evtl. nützlich sein.

## 011
### Wohlfühlen mit schönem und funktionierendem Design
Der Website-Konsument soll sich bei dem Besuch der WS wohlfühlen und schnell erfassen können worum es geht.

Geht bei beiden.

## 012
### OpenSource
Der Entwickler und Betreiber möchte Tools und Layouts mit OS-Lizenz, da das unkomplizierter ist.

Geht bei beiden.

## 013
### Leichtgewichtiger Buildprozess
Die Website-Betreiber sollen keine hohen Kosten beim Betreiben der Website haben.

Geht bei beiden.

Cryogen brauch evtl. GraalVM, aber tendenziell sind beide tools gleichwertig in Gewichtigkeit des Bau-Prozesses.

## 015
### Template-Eigenschaften sind überschreibbar
Als Website-Entwickler möchte ich Template Eigenschaften auf Ebene von Website oder Seite überschreiben können, damit ich kleine Änderungen schnell umsetzen kann.

Lokales überschreiben von Themes funktioniert:
Hugo:
    * Entweder über go modules
    * Oder lokale theme ordner
Cryogen:
    * Ebenfalls über lokale theme ordner

## 016
### Unser theme / layout soll privat bleiben können
Als meissa Mitglied wollen wir verhindern, dass jemand einfach unsere Website clonen kann.

Funktioniert bei beiden: Via git repo.

## 017
### URL-Redirects
Als Website-Pfleger möchte ich Redirects definieren können um eingägnge und stabile alternativ URLs zu einer Seite definieren zu können.

Kann cryogen nicht.
Unklar bei Hugo - Aliases?
Resourcen: https://gohugo.io/content-management/urls/

## 018
### Domain-Logik ist in Programiersprache beschreibbar.
Als Theme-Entwickler möchte ich möglichst wenig Logik in Templating haben müssen.
