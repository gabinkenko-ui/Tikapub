import 'dart:ui';

/// Dégradés (haut -> bas) pour les fonds générés par défaut.
/// Portage direct de `defaults.py::BACKGROUND_PALETTES` côté Python, pour garder
/// la même esthétique entre la version CLI et l'application mobile.
const List<(Color, Color)> kBackgroundPalettes = [
  (Color(0xFF141428), Color(0xFF461E5A)), // violet nuit
  (Color(0xFF0A1928), Color(0xFF0A5A6E)), // bleu océan
  (Color(0xFF230C0A), Color(0xFF783214)), // coucher de soleil
  (Color(0xFF0A2314), Color(0xFF0F5F3C)), // vert forêt
  (Color(0xFF1E0A23), Color(0xFF82145A)), // magenta néon
  (Color(0xFF0F0F0F), Color(0xFF373741)), // gris anthracite
];

/// Accords (fréquences en Hz) pour la nappe d'ambiance générée par défaut.
/// Portage direct de `defaults.py::AMBIENT_CHORDS`.
const List<(double, double, double)> kAmbientChords = [
  (130.81, 164.81, 196.00), // C3 majeur
  (146.83, 185.00, 220.00), // D3 majeur
  (164.81, 207.65, 246.94), // E3 majeur
  (196.00, 246.94, 293.66), // G3 majeur
  (110.00, 138.59, 164.81), // A2 majeur
];
