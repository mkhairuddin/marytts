/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.display;

import java.awt.Color;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.signalproc.analysis.FrameBasedAnalyser;
import marytts.util.data.AudioDoubleDataSource;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;


/**
 * @author Marc Schr&ouml;der
 *
 */
public class SilenceMarker extends Histogram
{
    public SilenceMarker(AudioInputStream ais) {
        this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
            
    public SilenceMarker(AudioInputStream ais, int width, int height) {
        super();
        if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
        }
        if (ais.getFormat().getChannels() > 1) {
            throw new IllegalArgumentException("Can only deal with mono audio signals");
        }
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        initialise(signal, samplingRate, width, height);
    }

    public SilenceMarker(double[] signal, int samplingRate)
    {
        this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SilenceMarker(double[] signal, int samplingRate, int width, int height)
    {
        initialise(new BufferedDoubleDataSource(signal), samplingRate, width, height);
    }
    
    protected void initialise(DoubleDataSource signal, int samplingRate, int width, int height)
    {
        showYAxis = false;
        showXAxis = false;
        paddingTop = 5;
        paddingBottom = 5;
        graphColor = Color.LIGHT_GRAY;
        histogramBorderColor = graphColor;
        double frameDuration = 0.01; // seconds
        int frameLength = (int) (samplingRate*frameDuration);
        int frameShift = frameLength / 2;
        if (frameLength%2==0) frameLength++; // make sure frame length is odd
        EnergyAnalyser energyAnalyser = new EnergyAnalyser(signal, frameLength, frameShift, samplingRate);
        double silenceCutoff = energyAnalyser.getSilenceCutoff();
        FrameBasedAnalyser.FrameAnalysisResult[] results = energyAnalyser.analyseAllFrames();
        double[] silenceData = new double[results.length];
        for (int i=0; i<results.length; i++) {
            double energy = ((Double)results[i].get()).doubleValue();
            if (energy <= silenceCutoff) silenceData[i] = 0.;
            else silenceData[i] = 1.;
        }
        super.initialise(width, height, 0, (double)frameShift/samplingRate, silenceData);
    }

    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
            SilenceMarker signalGraph = new SilenceMarker(ais);
            signalGraph.showInJFrame(args[i], true, false);
        }
    }
}