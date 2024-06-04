package com.clearcaptions.javawi.jstun.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CallTypeUtil
{
    public static Set<String> voipCallTypes = new HashSet<>(Arrays.asList("71", "171", "271"));

    private CallTypeUtil()
    {
    }

    public static boolean isVoipCallType(String callType)
    {
        return voipCallTypes.contains(callType);
    }
}
