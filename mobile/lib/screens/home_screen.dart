import 'package:flutter/material.dart';

import 'compilation_screen.dart';
import 'quote_screen.dart';
import 'voiceover_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tikapub')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text(
              'Crée ta vidéo, puis publie-la toi-même sur TikTok ou YouTube.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 32),
            _HomeButton(
              icon: Icons.format_quote,
              label: 'Citation',
              subtitle: 'Texte animé sur fond image, vidéo ou généré',
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const QuoteScreen()),
              ),
            ),
            const SizedBox(height: 16),
            _HomeButton(
              icon: Icons.record_voice_over,
              label: 'Voix off',
              subtitle: 'Script -> voix off hors-ligne -> sous-titres',
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const VoiceoverScreen()),
              ),
            ),
            const SizedBox(height: 16),
            _HomeButton(
              icon: Icons.video_library,
              label: 'Compilation',
              subtitle: 'Assemble tes propres clips en une vidéo',
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const CompilationScreen()),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HomeButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final String subtitle;
  final VoidCallback onTap;

  const _HomeButton({
    required this.icon,
    required this.label,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: Icon(icon, size: 32),
        title: Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
