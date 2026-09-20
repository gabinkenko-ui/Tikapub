import 'dart:io';

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

import '../services/save_share_service.dart';

/// Aperçu de la vidéo générée + enregistrement galerie / partage natif.
/// C'est ici que l'utilisateur publie lui-même sur TikTok ou YouTube, via la
/// feuille de partage native ou en retrouvant le fichier dans sa galerie.
class PreviewScreen extends StatefulWidget {
  final String videoPath;

  const PreviewScreen({super.key, required this.videoPath});

  @override
  State<PreviewScreen> createState() => _PreviewScreenState();
}

class _PreviewScreenState extends State<PreviewScreen> {
  late final VideoPlayerController _controller;
  final SaveShareService _saveShare = const SaveShareService();
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.file(File(widget.videoPath))
      ..initialize().then((_) {
        setState(() {});
        _controller.setLooping(true);
        _controller.play();
      });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _runBusy(Future<void> Function() action) async {
    setState(() => _busy = true);
    try {
      await action();
    } catch (e) {
      if (mounted) {
        // ignore: use_build_context_synchronously
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Aperçu')),
      body: Column(
        children: [
          Expanded(
            child: Center(
              child: _controller.value.isInitialized
                  ? AspectRatio(
                      aspectRatio: _controller.value.aspectRatio,
                      child: GestureDetector(
                        onTap: () => setState(
                          () => _controller.value.isPlaying ? _controller.pause() : _controller.play(),
                        ),
                        child: VideoPlayer(_controller),
                      ),
                    )
                  : const CircularProgressIndicator(),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    icon: const Icon(Icons.save_alt),
                    label: const Text('Enregistrer dans la galerie'),
                    onPressed: _busy
                        ? null
                        : () => _runBusy(() async {
                              await _saveShare.saveToGallery(widget.videoPath);
                              if (mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('Vidéo enregistrée dans la galerie.')),
                                );
                              }
                            }),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.share),
                    label: const Text('Partager'),
                    onPressed: _busy
                        ? null
                        : () => _runBusy(() => _saveShare.shareVideo(widget.videoPath)),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
