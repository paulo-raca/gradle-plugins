package testapp.android.gradle.inutilfutil.com.gradleplugintestapplication;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("foo", appContext.getString(R.string.noPlaceHolder));
        assertEquals("fooValue", appContext.getString(R.string.placeholder));
        assertEquals("${bar}", appContext.getString(R.string.invalidPlaceholder));
        assertEquals(appContext.getPackageName(), appContext.getString(R.string.appId));
    }
}
