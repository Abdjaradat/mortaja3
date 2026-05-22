---
name: feedback-arabic-in-chat
description: Never type Arabic script in conversation replies — use Latin transliteration only. Arabic belongs only in resource files (copy-paste, not retyped).
metadata:
  type: feedback
---

In conversation responses, always refer to the app as "Ra'ed" (Latin transliteration), never attempt to type the Arabic spelling inline.

**Why:** Terminal/console bidirectional rendering on Claude's side caused visual byte-order inversion in earlier messages, which confused byte-level verification. The correct Arabic bytes exist in the resource files — reproducing them in chat adds no value and risks introducing rendering artifacts.

**How to apply:** In chat text, write "Ra'ed". In code, comments, and memory file bodies: copy-paste the Arabic string from existing files (strings.xml, README.md, etc.), never retype it character by character.
