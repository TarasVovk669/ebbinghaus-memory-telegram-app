Objective:
Generate an engaging quiz of exactly 10 questions from the provided 'input_text'. Return results as JSON strictly matching the schema below.

Each question must include:
- type: "YES_NO", "SELECT", or "MISSING".
- text: clear and engaging question.
- variants: answer choices.
- correct_answer: key for the correct option.

Instructions:
1. Generate a balanced mix of at least 2 each: YES_NO, SELECT, MISSING.
2. If text is vocabulary-based, prioritize vocabulary in context.

Question types:
- YES_NO (max 3): variants ["true":"Yes", "false":"No"], nuanced questions.
- SELECT (min 4): variants ["A":"...", "B":"...", "C":"...", "D":"..."], plausible distractors.
- MISSING (min 3): vocabulary-focused, context-rich sentences, meaningful distractors.

Language Handling:
- If 'input_text' language differs from user's native 'language', generate quiz in user's 'language'.
- For vocab lists, use target language in MISSING questions; native language otherwise.

Error Handling:
Return "BAD_QUESTION_CANT_UNDERSTAND" or "TOO_SHORT" if applicable.

JSON Schema (exactly match this structure):

[
  "questions": [
    [
      "type": "YES_NO",
      "text": "Is the term 'quantum entanglement' translated correctly as 'квантова заплутаність'?",
      "variants": [
        "true": "Yes",
        "false": "No"
      ],
      "correct_answer": "true"
    ],
    [
      "type": "SELECT",
      "text": "Which element has the chemical symbol 'O'?",
      "variants": [
        "A": "Gold",
        "B": "Oxygen",
        "C": "Silver",
        "D": "Hydrogen"
      ],
      "correct_answer": "B"
    ],
    [
      "type": "MISSING",
      "text": "______ is confidently expressing beliefs and opinions clearly.",
      "variants": [
        "A": "Assertiveness",
        "B": "Bravery",
        "C": "Hesitation",
        "D": "Introversion"
      ],
      "correct_answer": "A"
    ]
    // +7 more questions
  ]
]

Input Parameters:
input_text: {input_text}, language: {language_code}
