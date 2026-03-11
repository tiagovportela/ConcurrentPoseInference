#!/usr/bin/env -S uv run
# /// script
# requires-python = ">=3.10"
# dependencies = ["opencv-python"]
# ///
"""Extract frames from a video file into a separate folder."""

import argparse
import sys
from pathlib import Path

import cv2


def extract_frames(video_path: str, output_dir: str, frame_interval: int = 1, max_frames: int | None = None) -> int:
    """Extract frames from a video file.

    Args:
        video_path: Path to the input video file.
        output_dir: Directory where frames will be saved.
        frame_interval: Save every Nth frame (1 = all frames).
        max_frames: Maximum number of frames to save (None = all).

    Returns:
        Number of frames saved.
    """
    video = Path(video_path)
    if not video.is_file():
        print(f"Error: video not found: {video_path}", file=sys.stderr)
        sys.exit(1)

    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)

    cap = cv2.VideoCapture(str(video))
    if not cap.isOpened():
        print(f"Error: could not open video: {video_path}", file=sys.stderr)
        sys.exit(1)

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    print(f"Video: {video.name} | {total_frames} frames | {fps:.2f} FPS")

    saved = 0
    frame_idx = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        if frame_idx % frame_interval == 0:
            filename = out / f"frame_{frame_idx:06d}.jpg"
            cv2.imwrite(str(filename), frame)
            saved += 1

            if max_frames is not None and saved >= max_frames:
                break

        frame_idx += 1

    cap.release()
    print(f"Saved {saved} frames to {out}")
    return saved


def main():
    parser = argparse.ArgumentParser(description="Extract frames from a video file.")
    parser.add_argument("video", help="Path to the input video file")
    parser.add_argument(
        "-o", "--output",
        help="Output directory (default: <video_name>_frames/)",
    )
    parser.add_argument(
        "-n", "--interval",
        type=int,
        default=1,
        help="Save every Nth frame (default: 1, i.e. all frames)",
    )
    parser.add_argument(
        "--max-frames",
        type=int,
        default=None,
        help="Only save the first N frames (default: all)",
    )
    args = parser.parse_args()

    output = args.output or f"{Path(args.video).stem}_frames"
    extract_frames(args.video, output, args.interval, args.max_frames)


if __name__ == "__main__":
    main()
