# Background music

Reviewed 2026-10-02 against BackgroundMusic, GameSettings, GameScreen and the shipped WAV files.

| Original supplied filename | Runtime filename | Current use |
| --- | --- | --- |
| 大厅背景音乐.wav | hub_theme.wav | Hub and Memorial Hall before all five stones |
| 战斗背景音.wav | battle_theme.wav | Training and all Battle maps |
| 通关音乐.wav | victory_theme.wav | Hub and Memorial Hall after all five distinct guardians |

The supplied files were renamed without re-encoding. All three are stereo, 48 kHz,
16-bit PCM WAV; durations are approximately 163.4, 152.0 and 198.0 seconds respectively.
The five-stone music change does not wait for the ending dialogue.

BackgroundMusic owns lazy, cached LibGDX Music streams for the gameplay screen. Tracks loop.
Selecting the same track across map changes continues playback without restarting it. Selecting
a different track starts that track from the beginning. Map transitions do not dispose streams;
screen disposal does. There is no crossfade or change to system volume.

Default gain is 50%. Title-menu Settings persists a 0–100 volume preference and fullscreen
in `${user.home}/.comp2120-game/settings.properties`; New Game/Continue uses that volume.
The Esc menu slider changes the current game's gain, applying to cached and future tracks
without restarting playback. Zero mutes. In-game restart retains that gain, but Esc changes do
not write the title preference; a later application launch uses the persisted setting.
Neither gameplay save slot contains volume/fullscreen.

In-game menus leave music playing for preview while gameplay pauses. Application lifecycle
pause pauses music. BackgroundMusicTest/MusicVolumeTest verify selection, gain, looping,
pausing and disposal through fake players. Actual device playback and transition continuity
still require listening in the native game. Current execution evidence is documented at
`Activity_2/docs/TEST_COVERAGE_REVIEW.md` from the repository root.

The desktop resources configuration packages this folder for IDE, Gradle and distributions.
Recorded provenance: generated using Suno during an active paid subscription; see
[asset attribution](../README.md) for the project's licensing boundary.
