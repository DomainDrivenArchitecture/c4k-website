# Aktoren

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

Cryogen: Hat Inhalte und Theme gemischt, Folder sind teilweise fest im Code verdrahtet

Hugo: Erlaubt Trennung von Inhalt und Theme, relativ viel Freiheit bei Folder-Structure (Erik sucht evtl. noch Details raus)

## 003
### Responsive Design
Als Website-Konsument möchte ich die Website auf Smartphon, Tablet oder auf dem 4K-Sreen konsumieren können.

## 004
### Inhalte ohne Layout
Der Website-Pfleger soll Inhalte möglichst einfach verändern können, ohne dass er sich um das Layout kümmern muss (evtl Markdown oder Asciidoc ?).

## 005
### Website muss keine Inhalte von extern downloaden (müssen) - z.B. Fonts, css etc
Als Website-Konsument möchte ich nur der besuchten Seite meine Daten zugestehen - und nicht der gesamten tracking-Welt.

## 007
### Rückwärtskompatibilität
Als Website-Betreiber möchte ich, dass alte Websites (bspw Informatikbüro Jerger) mit c4k-website weiterhin funktionieren, ohne bei ihnen Anpassungen machen zu müssen. (cluster muss weiterlaufen bei Änderungen)

## 008
### Website soll statisch sein
Als Website-Betreiber möchte ich eine statische Website ausliefern, damit der Website Betrieb nicht so komplex wird.

## 009
### Einfache und Zugängliche Technologie
Die Technologie, die der Website-Entwickler zum Bau der WS verwendet, soll gut dokumentiert und zugänglich sein. Damit ist es für den Website-Entwickler einfach, Änderungen im technischen Kontext der Website umzusetzen.

## 010
### Spass bei Entwicklung und Pflege
Website-Entwickler und Website-Pfleger sollten Spaß haben bei Website redesignen und Pflege (techn. und inhaltliche Pflege)

## 011
### Wohlfühlen mit schönem und funktionierendem Design
Der Website-Konsument soll sich bei dem Besuch der WS wohlfühlen und schnell erfassen können worum es geht.

## 012
### OpenSource
Der Entwickler und Betreiber möchte Tools und Layouts mit OS-Lizenz, da das unkomplizierter ist.

## 013
### Leichtgewichtiger Buildprozess
Die Website-Betreiber sollen keine hohen Kosten beim Betreiben der Website haben.

## 015
### Template-Eigenschaften sind überschreibbar
Als Website-Entwickler möchte ich Template Eigenschaften auf Ebene von Website oder Seite überschreiben können, damit ich kleine Änderungen schnell umsetzen kann.

## 16
### Unser theme / layout soll privat bleiben können
Als meissa Mitglied wollen wir verhindern, dass jemand einfach unsere Website clonen kann.