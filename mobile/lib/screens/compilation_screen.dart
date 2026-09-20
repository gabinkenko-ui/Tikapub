import 'dart:async';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../generators/compilation_generator.dart';
import '../services/media_picker_service.dart';
import '../widgets/generation_progress_dialog.dart';
import 'preview_screen.dart';

class CompilationScreen extends StatefulWidget {
  const CompilationScreen({super.key});

  @override
  State<CompilationScreen> createState() => _CompilationScreenState();
}

class _CompilationScreenState extends State<CompilationScreen> {
  final _introController = TextEditingController();
  final _picker = const MediaPickerService();

  final List<String> _clips = [];
  double _maxDuration = 45;
  bool _shuffle = false;
  String? _music;
  bool _generateDefaultMusic = true;
  bool _busy = false;

  @override
  void dispose() {
    _introController.dispose();
    super.dispose();
  }

  Future<void> _pickClips() async {
    final paths = await _picker.pickMultipleVideos();
    if (paths.isNotEmpty) setState(() => _clips.addAll(paths));
  }

  Future<void> _generate() async {
    if (_clips.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Choisis au moins un clip.')),
      );
      return;
    }

    setState(() => _busy = true);
    final progress = GenerationProgressController();
    unawaited(showGenerationProgressDialog(context, progress));

    try {
      final outputDir = await getApplicationDocumentsDirectory();
      final outputPath = '${outputDir.path}/tikapub_compilation_${const Uuid().v4()}.mp4';

      final result = await CompilationGenerator().generate(
        CompilationGenerationRequest(
          clipPaths: _clips,
          maxDuration: _maxDuration,
          introText: _introController.text.trim().isEmpty ? null : _introController.text.trim(),
          musicPath: _music,
          generateDefaultMusic: _generateDefaultMusic,
          shuffle: _shuffle,
        ),
        outputPath,
        onProgress: progress.update,
      );

      if (!mounted) return;
      Navigator.of(context).pop();
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => PreviewScreen(videoPath: result)),
      );
    } catch (e) {
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    } finally {
      progress.dispose();
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Compilation')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          OutlinedButton.icon(
            icon: const Icon(Icons.video_library),
            label: const Text('Choisir des clips'),
            onPressed: _pickClips,
          ),
          const SizedBox(height: 8),
          if (_clips.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 8),
              child: Text('Aucun clip sélectionné.'),
            )
          else
            ..._clips.asMap().entries.map(
                  (entry) => ListTile(
                    dense: true,
                    leading: const Icon(Icons.movie),
                    title: Text(entry.value.split('/').last, overflow: TextOverflow.ellipsis),
                    trailing: IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () => setState(() => _clips.removeAt(entry.key)),
                    ),
                  ),
                ),
          const Divider(height: 32),
          Text('Durée maximale : ${_maxDuration.round()} s'),
          Slider(
            value: _maxDuration,
            min: 15,
            max: 90,
            divisions: 75,
            label: '${_maxDuration.round()} s',
            onChanged: (v) => setState(() => _maxDuration = v),
          ),
          TextField(
            controller: _introController,
            decoration: const InputDecoration(
              labelText: "Titre d'intro (optionnel)",
              border: OutlineInputBorder(),
            ),
          ),
          SwitchListTile(
            title: const Text('Mélanger l\'ordre des clips'),
            value: _shuffle,
            onChanged: (v) => setState(() => _shuffle = v),
          ),
          const Divider(height: 32),
          ListTile(
            leading: const Icon(Icons.music_note),
            title: const Text('Musique'),
            subtitle: Text(_music?.split('/').last ?? 'Aucun fichier choisi'),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (_music != null)
                  IconButton(icon: const Icon(Icons.close), onPressed: () => setState(() => _music = null)),
                TextButton(
                  onPressed: () async {
                    final path = await _picker.pickAudio();
                    if (path != null) setState(() => _music = path);
                  },
                  child: const Text('Choisir'),
                ),
              ],
            ),
          ),
          if (_music == null)
            SwitchListTile(
              title: const Text('Générer une musique d\'ambiance automatiquement'),
              value: _generateDefaultMusic,
              onChanged: (v) => setState(() => _generateDefaultMusic = v),
            ),
          const SizedBox(height: 24),
          FilledButton.icon(
            icon: const Icon(Icons.movie_creation),
            label: const Text('Générer la vidéo'),
            onPressed: _busy ? null : _generate,
          ),
        ],
      ),
    );
  }
}
