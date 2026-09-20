import 'package:flutter/material.dart';

/// Ligne "choisir un fichier" réutilisable (fond image/vidéo, musique, clips).
class MediaPickerTile extends StatelessWidget {
  final String label;
  final String? selectedPath;
  final IconData icon;
  final VoidCallback onPick;
  final VoidCallback? onClear;

  const MediaPickerTile({
    super.key,
    required this.label,
    required this.selectedPath,
    required this.icon,
    required this.onPick,
    this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final fileName = selectedPath?.split('/').last;
    return ListTile(
      leading: Icon(icon),
      title: Text(label),
      subtitle: fileName != null ? Text(fileName, overflow: TextOverflow.ellipsis) : const Text('Aucun fichier choisi'),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (selectedPath != null && onClear != null)
            IconButton(icon: const Icon(Icons.close), onPressed: onClear),
          TextButton(onPressed: onPick, child: const Text('Choisir')),
        ],
      ),
    );
  }
}
