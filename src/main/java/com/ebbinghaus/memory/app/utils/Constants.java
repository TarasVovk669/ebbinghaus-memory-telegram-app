package com.ebbinghaus.memory.app.utils;

import com.ebbinghaus.memory.app.model.LanguageData;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Constants {

  public static final String DEFAULT_LANGUAGE_CODE = "en";
  public static final int DEFAULT_DATA_PAGE_SIZE = 5;
  public static final int DEFAULT_CATEGORY_PAGE_SIZE = 5;
  public static final long ZERO_COUNT = 0L;
  public static final int TRY_COUNT = 1;
  public static final Long DEFAULT_TIME_EXECUTION = 30L;
  public static final long MINUTES_IN_HOUR = 60;
  public static final long MINUTES_IN_DAY = 1440; // 24 * 60
  public static final long MINUTES_IN_YEAR = 525960; // 365.25 * 24 * 60
  public static final long MINUTES_IN_MONTH = 43830; // 30.44 * 24 * 60

  public static final String EDIT_CONCRETE_MESSAGE_CALLBACK = "ccm";
  public static final String VIEW_MESSAGE_CALLBACK = "vm";
  public static final String VIEW_SHORT_MESSAGE_CALLBACK = "vsm";
  public static final String EDIT_MESSAGE_CALLBACK = "cm";
  public static final String BACK_MESSAGE_CALLBACK = "bm";
  public static final String BACK_FULL_MESSAGE_CALLBACK = "bfm";
  public static final String NAVIGATION_DATA_LIST_CALLBACK = "ndl";
  public static final String NAVIGATION_CATEGORY_LIST_CALLBACK = "ncl";
  public static final String DELETE_MESSAGE_CALLBACK = "dm";
  public static final String RESTART_MESSAGE_CALLBACK = "rmc";
  public static final String TEST_MESSAGE_CALLBACK = "tm";
  public static final String DELETE_MESSAGE_YES_CALLBACK = "dmy";
  public static final String RESTART_MESSAGE_YES_CALLBACK = "rmy";
  public static final String DELETE_MESSAGE_NO_CALLBACK = "dmn";
  public static final String RESTART_MESSAGE_NO_CALLBACK = "rmn";
  public static final String VIEW_PROFILE_LANGUAGE_CALLBACK = "vpl";
  public static final String HOT_IT_WORKS_CALLBACK = "htw";
  public static final String PROFILE_MAIN_MENU_CALLBACK = "pmm";
  public static final String CHANGE_PROFILE_LANGUAGE_CALLBACK = "cpl";
  public static final String CONTACT_INFO_CALLBACK = "cic";
  public static final String QUIZ_QUESTION_CALLBACK = "qqc";
  public static final String QUIZ_NEXT_QUESTION_CALLBACK = "qnqc";

  public static final String UNCATEGORIZED = "#uncategorized";
  public static final String FORWARDED = "#forwarded";
  public static final int FIRST_EXECUTION_STEP = 1;
  public static final String INFO_LIST = "\uD83D\uDCCB\u00A0";
  public static final String CATEGORY_LIST = "\uD83C\uDFAF\u00A0";
  public static final String PROFILE_LIST = "\uD83D\uDC64\u00A0";
  public static final String ADD_NEW_INFO = "➕\u00A0";

  public static final String START = "/start";
  public static final String HELP = "/help";
  public static final String MENU = "/menu";
  public static final int SHORT_MESSAGE_SYMBOL_QUANTITY = 300;

  public static final String DOTS_STR = "...";
  public static final int SHORT_ELEMENT_LENGTH = 100;

  public static final String BOLD_STYLE = "bold";
  public static final String OPERATION = "O";
  public static final String QUIZ_QUESTION_ID = "QQ";
  public static final String QUIZ_ID = "QID";
  public static final String QUIZ_ANSWER = "QA";
  public static final String LANGUAGE_CODE = "LC";
  public static final String MESSAGE_ID = "M";
  public static final String CATEGORY_ID = "C";
  public static final String IS_BACK = "IB";
  public static final String CATEGORY_PAGE = "CP";
  public static final String CATEGORY_SIZE = "CS";
  public static final String PAGE = "P";
  public static final String SIZE = "S";

  public static final String FIB_STEP_FIRST = "fib_step_first";
  public static final String FIB_STEP_SECOND = "fib_step_second";
  public static final String TRIGGERS_GROUP = "message-triggers";
  public static final String JOBS_GROUP = "message-jobs";
  public static final int MINIMUM_TEST_PASSED_LENGTH = 500;
  public static final String MESSAGE_CAN_T_BE_DELETED_FOR_EVERYONE =
      "message can't be deleted for everyone";
  public static final String MARKDOWN = "markdown";
  public static final List<String> SERVER_MOST_POPULAR_ERRORS =
      List.of("500", "501", "502", "503", "504");
  public static final Map<String, AtomicInteger> COUNT_MAP =
      new ConcurrentReferenceHashMap<>(100, ConcurrentReferenceHashMap.ReferenceType.WEAK);
  public static final TypeReference<HashMap<String, String>> MAP_TYPE_REF =
      new TypeReference<>() {};
  public static final Map<String, String> IMAGE_CACHE_MAP = new HashMap<>();
  public static final Map<Integer, Long> INTERVAL_MAP =
      Map.ofEntries(
          Map.entry(1, 30L),
          Map.entry(2, 60 * 8L),
          Map.entry(3, 60 * 24L),
          Map.entry(4, 7 * 60 * 24L),
          Map.entry(5, 30 * 60 * 24L),
          Map.entry(6, 3 * 30 * 60 * 24L),
          Map.entry(7, 6 * 30 * 60 * 24L),
          Map.entry(8, 12 * 30 * 60 * 24L),
          Map.entry(9, 24 * 30 * 60 * 24L),
          Map.entry(10, 60 * 30 * 60 * 24L));
  public static final Map<String, LanguageData> AVAILABLE_LANGUAGES_MAP =
      Map.ofEntries(
          Map.entry("en", new LanguageData("\uD83C\uDDFA\uD83C\uDDF8", "English")),
          Map.entry("uk", new LanguageData("\uD83C\uDDFA\uD83C\uDDE6", "Українська")),
          Map.entry("ru", new LanguageData("\uD83C\uDDF7\uD83C\uDDFA", "Русский")));

  public static final String PROMPT =
      """
            Create a challenging quiz from the given text, returning a JSON object with 10 questions.
            Each question must include:

            type: YES_NO, SELECT, MISSING
            text: The question text
            variants: Answer options
            correct_answer: The correct answer
            Questions should be difficult, relevant, and varied. Analyze the input text for main ideas and themes. If insufficient information, generate some questions independently. If vocabulary-based, include MISSING questions with context-based options.

            Use the language specified in language_code.
            Translate questions based on {input_text} language AND on {language_code} input parameter!
            If the input text is not understandable, include "error": "BAD_QUESTION_CANT_UNDERSTAND" in the response. If the text is too short, include "error": "TOO_SHORT".
            Response must contains ONLY "questions" OR "error"!

            For answers:

            YES_NO: {"true": "Yes", "false": "No"}
            SELECT and MISSING: {"A": "element 1", "B": "element 2", "C": "element 3", "D": "element 4"}
            Make YES_NO questions more difficult, with the frequency of true answers lower than false. If language learning is indicated, MISSING questions should be in the learning language, while the rest are in language_code.

            Input parameters:

            language_code: Language code for the quiz.
            input_text: Text for the quiz
            JSON schema for the response:

            json
            {
              "error": "BAD_QUESTION_CANT_UNDERSTAND",
              "questions": [
                {
                  "type": "YES_NO",
                  "text": "Is the term 'quantum entanglement' accurately translated to 'квантова заплутаність'?",
                  "variants": {
                    "true": "Yes",
                    "false": "No"
                  },
                  "correct_answer": "true"
                },
                {
                  "type": "YES_NO",
                  "text": "Does the word 'ineffable' mean 'easily expressed'?",
                  "variants": {
                    "true": "Yes",
                    "false": "No"
                  },
                  "correct_answer": "false"
                },
                {
                  "type": "SELECT",
                  "text": "Question text",
                  "variants": {
                    "A": "Option 1",
                    "B": "Option 2",
                    "C": "Option 3",
                    "D": "Option 4"
                  },
                  "correct_answer": "A"
                },
                {
                  "type": "MISSING",
                  "text": "______ is the quality of being confident and not afraid to say what you want or believe.",
                  "variants": {
                    "A": "Bravery",
                    "B": "Assertiveness",
                    "C": "Fear",
                    "D": "Timidity"
                  },
                  "correct_answer": "B"
                }
                // 6 more questions in similar format
              ]
            }
            This is language_code: {%s} and input_text: {%s}. Return only JSON!
            """;
}
