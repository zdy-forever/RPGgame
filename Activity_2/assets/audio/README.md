# Background music

Reviewed 2026-09-30: potion HUD, input feedback and save/load onboarding do not change music
selection, gain or looping. BackgroundMusicTest and MusicVolumeTest cover the model/fake-player
contract; [current verification status](../../docs/TEST_COVERAGE_REVIEW.md) distinguishes this
from actual audio-device and uninterrupted-playback checks.

The user-supplied WAV files were renamed without re-encoding or modifying their audio content:

| Original filename | Runtime filename | Use |
| --- | --- | --- |
| 大厅背景音乐.wav | hub_theme.wav | Hub and Memorial Hall before five stones |
| 战斗背景音.wav | battle_theme.wav | Training and all Battle maps |
| 通关音乐.wav | victory_theme.wav | Hub and Memorial Hall after five stones |

All tracks are stereo 48 kHz, 16-bit PCM WAV. Approximate durations are 163.4, 152 and 198 seconds,
respectively. BackgroundMusic uses streamed, looping LibGDX Music playback and a centralized 50% gain.
The same track continues across map changes; map transitions do not own or dispose audio.
Switching to a different track starts that song from the beginning. The Esc menu provides a
0–100 music-volume slider (mouse drag or focused Left/Right arrows). Gain changes apply to
cached and future tracks without restarting the song. Menu pause leaves audio playing for
preview; lifecycle pause still pauses it. In-game restart retains gain; a new application
launch uses the default 50%. No crossfade, system-volume change or disk preference is added.

The existing desktop resources configuration includes this folder for IDEA, Gradle and distributions.
All tracks were generated using Suno during an active paid subscription with commercial-use rights; see `../README.md` for licensing/attribution details.
