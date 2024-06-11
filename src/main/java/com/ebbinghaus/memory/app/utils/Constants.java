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

    public static final String EDIT_CONCRETE_MESSAGE_CALLBACK = "ccm";
    public static final String VIEW_MESSAGE_CALLBACK = "vm";
    public static final String VIEW_SHORT_MESSAGE_CALLBACK = "vsm";
    public static final String EDIT_MESSAGE_CALLBACK = "cm";
    public static final String BACK_MESSAGE_CALLBACK = "bm";
    public static final String NAVIGATION_DATA_LIST_CALLBACK = "ndl";
    public static final String NAVIGATION_CATEGORY_LIST_CALLBACK = "ncl";
    public static final String DELETE_MESSAGE_CALLBACK = "dm";
    public static final String DELETE_MESSAGE_YES_CALLBACK = "dmy";
    public static final String DELETE_MESSAGE_NO_CALLBACK = "dmn";
    public static final String VIEW_PROFILE_LANGUAGE_CALLBACK = "vpl";
    public static final String HOT_IT_WORKS_CALLBACK = "htw";
    public static final String PROFILE_MAIN_MENU_CALLBACK = "pmm";
    public static final String CHANGE_PROFILE_LANGUAGE_CALLBACK = "cpl";
    public static final String CONTACT_INFO_CALLBACK = "cic";

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
    public static final String LANGUAGE_CODE = "LC";
    public static final String MESSAGE_ID = "M";
    public static final String CATEGORY_ID = "C";
    public static final String IS_BACK = "IB";
    public static final String CATEGORY_PAGE = "CP";
    public static final String CATEGORY_SIZE = "CS";
    public static final String PAGE = "P";
    public static final String SIZE = "S";

    public final static List<String> SERVER_MOST_POPULAR_ERRORS = List.of("500", "501", "502", "503", "504");
    public final static Map<String, AtomicInteger> COUNT_MAP = new ConcurrentReferenceHashMap<>(100, ConcurrentReferenceHashMap.ReferenceType.WEAK);
    public static final TypeReference<HashMap<String, String>> MAP_TYPE_REF = new TypeReference<>() {
    };
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
                    Map.entry(10, 60 * 30 * 60 * 24L)
            );

    public static final Map<String, LanguageData> AVAILABLE_LANGUAGES_MAP =
            Map.ofEntries(
                    Map.entry("en", new LanguageData("\uD83C\uDDFA\uD83C\uDDF8", "English")),
                    Map.entry("uk", new LanguageData("\uD83C\uDDFA\uD83C\uDDE6", "Українська")),
                    Map.entry("ru", new LanguageData("\uD83C\uDDF7\uD83C\uDDFA", "Русский"))
            );
}
