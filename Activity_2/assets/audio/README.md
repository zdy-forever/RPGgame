# Background music

The user-supplied WAV files were renamed without re-encoding or modifying their audio content:

| Original filename | Runtime filename | Use |
| --- | --- | --- |
| 大厅背景音乐.wav | hub_theme.wav | Hub and Memorial Hall before five stones |
| 战斗背景音.wav | battle_theme.wav | Training and all Battle maps |
| 通关音乐.wav | victory_theme.wav | Hub and Memorial Hall after five stones |

All tracks are stereo 48 kHz, 16-bit PCM WAV. Approximate durations are 163.4, 152 and 198 seconds,
respectively. BackgroundMusic uses streamed, looping LibGDX Music playback and a centralized 50% gain.
The same track continues across map changes; map transitions do not own or dispose audio.
Switching to a different track starts that song from the beginning. No crossfade or volume UI is added.

The existing desktop resources configuration includes this folder for IDEA, Gradle and distributions.
Audio authorship/licensing was not supplied; these filenames are not a claim of authorship or licence.
