/**
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.common;

import ee.ria.xroad.common.util.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audit log.
 */
@Slf4j
public final class AuditLogger {

    public static final String XROAD_USER = "xroad";
    private static final String SYSTEM_USER = "system";

    private static final String EVENT_PARAM = "event";
    private static final String USER_PARAM = "user";
    private static final String REASON_PARAM = "reason";
    private static final String UNHANDLED_WARNING_PARAM = "warning";
    private static final String AUTH_PARAM = "auth";
    private static final String URL_PARAM = "url";
    private static final String DATA_PARAM = "data";

    private static final String FAILURE_SUFFIX = " failed";

    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("AUDIT");

    private AuditLogger() {
    }

    /**
     * Log an event in JSON format.
     * @param jsonMessage message in JSON format
     */
    public static void log(String jsonMessage) {
        log.info(AUDIT_MARKER, jsonMessage);
    }

    /**
     * Log an event with data where the user is 'system'.
     * @param event logged event
     * @param data relevant details of the event
     */
    public static void log(String event, Map<String, Object> data) {
        log(event, SYSTEM_USER, data);
    }

    /**
     * Log an event with data for a user.
     * @param event logged event
     * @param user the user who initiated the event
     * @param data relevant details of the event
     */
    public static void log(String event, String user, Map<String, Object> data) {
        Map<String, Object> message = createSuccessMessageMap(event, user, data, null, null);
        log(serializeJson(message));
    }

    /**
     * Log a (non-warning) failure event with data for a user.
     * @param event logged event (suffix " failed" is added to the event)
     * @param user the user who initiated the event
     * @param reason the reason of the failure
     * @param data relevant details of the event
     */
    public static void log(String event, String user, String reason,
            Map<String, Object> data) {
        Map<String, Object> message = createMessageMap(event, user, reason, data, null, null, true, false);
        log(serializeJson(message));
    }

    /**
     * Audit log a success message, with authentication type information
     * @param event
     * @param user
     * @param data
     * @param auth
     * @param url
     */
    public static void log(String event, String user, Map<String, Object> data, String auth, String url) {
        Map<String, Object> message = createSuccessMessageMap(event, user, data, auth, url);
        log(serializeJson(message));
    }

    /**
     * Audit log a failure message, with authentication type information
     * @param event
     * @param user
     * @param reason
     * @param data
     * @param auth
     * @param url
     */
    public static void log(String event, String user, String reason, Map<String, Object> data,
            String auth, String url) {
        Map<String, Object> message = createMessageMap(event, user, reason, data, auth, url, true, false);
        log(serializeJson(message));
    }

    /**
     * Audit log a failure message about unhandled warnings, with authentication type information
     * @param event
     * @param user
     * @param reason
     * @param data
     * @param auth
     * @param url
     */
    public static void logWarning(String event, String user, String reason, Map<String, Object> data,
            String auth, String url) {
        Map<String, Object> message = createMessageMap(event, user, reason, data, auth, url, true, true);
        log(serializeJson(message));
    }

    private static String serializeJson(Map<String, Object> message) {
        // serialize nulls, like old Ruby implementation
        return JsonUtils.getSerializer(true).toJson(message);
    }

    // message map for successful event (no reason)
    private static Map<String, Object> createSuccessMessageMap(String event, String user,
            Map<String, Object> data, String auth, String url) {
        return createMessageMap(event, user, null, data, auth, url, false, false);
    }

    /**
     * @param event raw event name. " failure" postfix will be added for failures
     * @param user user, always included (even if null)
     * @param reason possible reason, only included if not null
     * @param data data, always included (even if null)
     * @param auth possible authentication type, only included if not null
     * @param url possible url, only included if not null
     * @param isFailure if true, this is about a failed event
     * @param isWarning if true, include boolean that indicates this failure event is about unhandled warnings
     */
    private static Map<String, Object> createMessageMap(String event, String user, String reason,
            Map<String, Object> data, String auth, String url, boolean isFailure, boolean isWarning) {
        if (!isFailure && isWarning) {
            throw new IllegalArgumentException("illegal parameter (!isFailure && isWarning)");
        }
        Map<String, Object> message = new LinkedHashMap<>();
        String eventName = event;
        if (isFailure) {
            eventName = event + FAILURE_SUFFIX;
        }
        message.put(EVENT_PARAM, eventName);
        message.put(USER_PARAM, user);
        if (reason != null) {
            message.put(REASON_PARAM, reason);
        }
        if (isFailure) {
            message.put(UNHANDLED_WARNING_PARAM, isWarning);
        }
        if (auth != null) {
            message.put(AUTH_PARAM, auth);
        }
        if (url != null) {
            message.put(URL_PARAM, url);
        }
        message.put(DATA_PARAM, data);
        return message;
    }

}
