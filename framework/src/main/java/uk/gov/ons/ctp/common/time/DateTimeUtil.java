package uk.gov.ons.ctp.common.time;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;

// import net.sourceforge.cobertura.CoverageIgnore;

/** Centralized DateTime handling for CTP */
// @CoverageIgnore
@Slf4j
public class DateTimeUtil {

  public static final String DATE_FORMAT_IN_JSON = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

  private static DateTimeFormatter dateTimeFormatterForJson =
      DateTimeFormatter.ofPattern(DATE_FORMAT_IN_JSON).withZone(ZoneId.systemDefault());

  /**
   * Looks like overkill I know - but this ensures that we consistently stamp model objects with UTC
   * datetime
   *
   * @return The current time in UTC
   */
  public static Timestamp nowUTC() {
    return new Timestamp(System.currentTimeMillis());
  }

  /**
   * To get a XMLGregorianCalendar for now
   *
   * @return a XMLGregorianCalendar for now
   * @throws DatatypeConfigurationException if it can't create a calendar
   */
  public static XMLGregorianCalendar giveMeCalendarForNow() throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(new Date());

    XMLGregorianCalendar result = null;
    DatatypeFactory factory = DatatypeFactory.newInstance();
    result =
        factory.newXMLGregorianCalendar(
            gregorianCalendar.get(GregorianCalendar.YEAR),
            gregorianCalendar.get(GregorianCalendar.MONTH) + 1,
            gregorianCalendar.get(GregorianCalendar.DAY_OF_MONTH),
            gregorianCalendar.get(GregorianCalendar.HOUR_OF_DAY),
            gregorianCalendar.get(GregorianCalendar.MINUTE),
            gregorianCalendar.get(GregorianCalendar.SECOND),
            gregorianCalendar.get(GregorianCalendar.MILLISECOND),
            0);
    return result;
  }

  /**
   * To transform a string into XMLGregorianCalendar. If it fails building a XMLGregorianCalendar
   * from the string, it will build a XMLGregorianCalendar for now.
   *
   * @param string the string to transform
   * @param format the format used to parse the string
   * @return the XMLGregorianCalendar
   * @throws DatatypeConfigurationException when a XMLGregorianCalendar for now cannot be built
   */
  public static XMLGregorianCalendar stringToXMLGregorianCalendar(String string, String format)
      throws DatatypeConfigurationException {
    XMLGregorianCalendar result = null;
    Date date;
    SimpleDateFormat simpleDateFormat;
    GregorianCalendar gregorianCalendar;

    simpleDateFormat = new SimpleDateFormat(format);
    try {
      date = simpleDateFormat.parse(string);
      gregorianCalendar = (GregorianCalendar) GregorianCalendar.getInstance();
      gregorianCalendar.setTime(date);
      result = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    } catch (ParseException e) {
      log.error("Failed to parse date", e);
      result = DateTimeUtil.giveMeCalendarForNow();
    }

    return result;
  }

  /**
   * Create a String containing the current time formatted for use in JSON.
   *
   * @return a String formatted as per DateTimeUtil.DATE_FORMAT_IN_JSON.
   */
  public static String getCurrentDateTimeInJsonFormat() {
    return dateTimeFormatterForJson.format(ZonedDateTime.now());
  }

  /**
   * Format the supplied Date using the the standard JSON timestamp format (as defined in
   * DATE_FORMAT_IN_JSON)
   *
   * @param date is the date to convert to a String.
   * @return a String containing the date formatted to the standard JSON format.
   */
  public static String formatDate(Date date) {
    return dateTimeFormatterForJson.format(date.toInstant());
  }
}
