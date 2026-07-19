import 'dart:math' as math;
import 'package:flutter/material.dart';

/// A circular progress ring with a smooth gradient sweep and an optional
/// gentle "breathing" glow — used for urgent (< 24h) events. Unlike a home
/// screen widget, this runs live inside the app, so the pulse is a real,
/// continuous animation rather than an approximation.
class PulsingProgressRing extends StatefulWidget {
  final double progress; // 0..1
  final Color colorStart;
  final Color colorEnd;
  final double size;
  final double strokeWidth;
  final bool pulse;

  const PulsingProgressRing({
    super.key,
    required this.progress,
    required this.colorStart,
    required this.colorEnd,
    this.size = 48,
    this.strokeWidth = 4,
    this.pulse = false,
  });

  @override
  State<PulsingProgressRing> createState() => _PulsingProgressRingState();
}

class _PulsingProgressRingState extends State<PulsingProgressRing>
    with SingleTickerProviderStateMixin {
  AnimationController? _controller;

  @override
  void initState() {
    super.initState();
    _syncController();
  }

  @override
  void didUpdateWidget(covariant PulsingProgressRing oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.pulse != widget.pulse) _syncController();
  }

  void _syncController() {
    if (widget.pulse && _controller == null) {
      _controller = AnimationController(
        vsync: this,
        duration: const Duration(milliseconds: 1400),
      )..repeat(reverse: true);
    } else if (!widget.pulse && _controller != null) {
      _controller!.dispose();
      _controller = null;
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final controller = _controller;
    return SizedBox(
      width: widget.size,
      height: widget.size,
      child: controller == null
          ? CustomPaint(
              painter: _RingPainter(
                progress: widget.progress,
                colorStart: widget.colorStart,
                colorEnd: widget.colorEnd,
                strokeWidth: widget.strokeWidth,
                glow: 0,
              ),
            )
          : AnimatedBuilder(
              animation: controller,
              builder: (context, _) => CustomPaint(
                painter: _RingPainter(
                  progress: widget.progress,
                  colorStart: widget.colorStart,
                  colorEnd: widget.colorEnd,
                  strokeWidth: widget.strokeWidth,
                  glow: Curves.easeInOut.transform(controller.value),
                ),
              ),
            ),
    );
  }
}

class _RingPainter extends CustomPainter {
  final double progress;
  final Color colorStart;
  final Color colorEnd;
  final double strokeWidth;
  final double glow;

  _RingPainter({
    required this.progress,
    required this.colorStart,
    required this.colorEnd,
    required this.strokeWidth,
    required this.glow,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (math.min(size.width, size.height) - strokeWidth) / 2;
    final rect = Rect.fromCircle(center: center, radius: radius);
    final sweepAngle = 2 * math.pi * progress.clamp(0.0, 1.0);

    if (glow > 0) {
      final glowPaint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth * 2.2
        ..strokeCap = StrokeCap.round
        ..color = colorEnd.withOpacity(0.10 + 0.28 * glow)
        ..maskFilter = MaskFilter.blur(BlurStyle.normal, strokeWidth * 1.6);
      canvas.drawArc(rect, -math.pi / 2, sweepAngle, false, glowPaint);
    }

    final trackPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..color = colorStart.withOpacity(0.14);
    canvas.drawCircle(center, radius, trackPaint);

    final progressPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..shader = SweepGradient(
        colors: [colorStart, colorEnd, colorStart],
        stops: const [0, 0.5, 1],
        transform: const GradientRotation(-math.pi / 2),
      ).createShader(rect);
    canvas.drawArc(rect, -math.pi / 2, sweepAngle, false, progressPaint);
  }

  @override
  bool shouldRepaint(covariant _RingPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.colorStart != colorStart ||
        oldDelegate.colorEnd != colorEnd ||
        oldDelegate.strokeWidth != strokeWidth ||
        oldDelegate.glow != glow;
  }
}
