/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class IntellitracProtocolDecoder extends BaseProtocolDecoder {

    public IntellitracProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    static private Pattern pattern = Pattern.compile(
            "(?:.+,)?(\\d+)," +            // Device Identifier
            "(\\d{4})(\\d{2})(\\d{2})" +   // Date (YYYYMMDD)
            "(\\d{2})(\\d{2})(\\d{2})," +  // Time (HHMMSS)
            "(\\d+\\.\\d+)," +             // Longitude
            "(\\d+\\.\\d+)," +             // Latitude
            "(\\d+\\.?\\d*)," +            // Speed
            "(\\d+\\.?\\d*)," +            // Course
            "(\\d+\\.?\\d*)," +            // Altitude
            "(\\d+)," +                    // Satellites
            "(\\d+)," +                    // Report Identifier
            "(\\d+)," +                    // Input
            "(\\d+),?" +                   // Output
            "(\\d+\\.\\d+)?,?" +           // ADC1
            "(\\d+\\.\\d+)?");             // ADC2

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;
        
        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        StringBuilder extendedInfo = new StringBuilder("<protocol>intellitrac</protocol>");
        Integer index = 1;

        // Detect device
        String id = parser.group(index++);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
            return null;
        }
        
        // Date and time
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        position.setTime(time.getTime());
        
        // Location data
        position.setLongitude(Double.valueOf(parser.group(index++)));
        position.setLatitude(Double.valueOf(parser.group(index++)));
        position.setSpeed(Double.valueOf(parser.group(index++)));
        position.setCourse(Double.valueOf(parser.group(index++)));
        position.setAltitude(Double.valueOf(parser.group(index++)));
        
        // Satellites
        int satellites = Integer.valueOf(parser.group(index++));
        position.setValid(satellites >= 3);
        extendedInfo.append("<satellites>");
        extendedInfo.append(satellites);
        extendedInfo.append("</satellites>");
        
        // Report identifier
        position.setId(Long.valueOf(parser.group(index++)));

        // Input
        extendedInfo.append("<input>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</input>");

        // Output
        extendedInfo.append("<output>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</output>");

        // ADC1
        String adc1 = parser.group(index++);
        if (adc1 != null) {
            extendedInfo.append("<adc1>").append(adc1).append("</adc1>");
        }

        // ADC2
        String adc2 = parser.group(index++);
        if (adc2 != null) {
            extendedInfo.append("<adc2>").append(adc2).append("</adc2>");
        }

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
