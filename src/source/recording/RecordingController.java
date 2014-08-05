package source.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

import log.Log;
import source.SourceException;
import source.tuner.FrequencyChangeListener;
import source.tuner.FrequencyController;
import source.tuner.FrequencyController.Tunable;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import controller.ThreadPoolManager;

public class RecordingController implements Tunable {
	// TODO: Implement values for more than just SBX daughterboard (400-4400 MHz)
	public static final int sMINIMUM_TUNABLE_FREQUENCY = 400000000;
	public static final int sMAXIMUM_TUNABLE_FREQUENCY = 2050000000; // TODO: Actual value is out of maximum range
	public static final int sSAMPLE_RATE = 192000; // TODO: write gnuradio script (using rational resampler) to generate this sample rate
	
	/* List of currently tuned channels being served to demod channels */
	protected ArrayList<TunerChannel> mTunedChannels = 
			new ArrayList<TunerChannel>();
	
	protected FrequencyController mFrequencyController;
	
	/**
	 * The recording controller manages the frequency and bandwidth of the
	 * currently tuned channels on the recording.
	 *  
	 * @param minimumFrequency - minimum uncorrected tunable frequency
	 * @param maximumFrequency - maximum uncorrected tunable frequency
	 * @throws SourceException - for any issues related to constructing the 
	 * class, tuning a frequency, or setting the bandwidth
	 */
	public RecordingController(long minimumFrequency, long maximumFrequency) 
			throws SourceException
	{
		mFrequencyController = new FrequencyController( this,
				minimumFrequency,
				maximumFrequency,
				0.0d);
	}
	
	public RecordingController() 
			throws SourceException
	{
		this(sMINIMUM_TUNABLE_FREQUENCY, sMAXIMUM_TUNABLE_FREQUENCY);
	}
	
	public int getBandwidth() {
		return mFrequencyController.getBandwidth();
	}
	
	/**
     * Sets the center frequency of the local oscillator.
     * 
     * @param frequency in hertz
     * @throws SourceException - if the tuner has any issues
     */
	public void setFrequency( long frequency ) throws SourceException
	{
		mFrequencyController.setFrequency( frequency );
	}

	/**
	 * Gets the center frequency of the local oscillator
	 * 
	 * @return frequency in hertz
	 */
	public long getFrequency()	
	{
		return mFrequencyController.getFrequency();
	}

	public double getFrequencyCorrection()
	{
		return mFrequencyController.getFrequencyCorrection();
	}
	
	public void setFrequencyCorrection( double correction ) throws SourceException
	{
		mFrequencyController.setFrequencyCorrection( correction );
	}
	
	public long getMinFrequency()
	{
		return mFrequencyController.getMinimumFrequency();
	}

	public long getMaxFrequency()
	{
		return mFrequencyController.getMaximumFrequency();
	}

	@Override
	public long getTunedFrequency() throws SourceException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Indicates if the tuner can accomodate this new channel frequency and
	 * bandwidth, along with all of the existing tuned channels currently in 
	 * place.
	 */
	public boolean canTuneChannel( TunerChannel channel )
	{
		//Make sure we're within the tunable frequency range of this tuner
		if( getMinFrequency() < channel.getMinFrequency() &&
			getMaxFrequency() > channel.getMaxFrequency() )
		{
			//If this is the first lock, then we're good
			if( mTunedChannels.isEmpty() )
			{
				return true;
			}
			else
			{
				//Sort the existing locks and get the min/max locked frequencies
				Collections.sort( mTunedChannels );

				long minLockedFrequency = mTunedChannels.get( 0 ).getMinFrequency();
				long maxLockedFrequency = mTunedChannels
						.get( mTunedChannels.size() - 1 ).getMaxFrequency();

				//Requested channel is higher than min locked frequency
				if( minLockedFrequency <= channel.getMinFrequency() &&
					( channel.getMaxFrequency() - minLockedFrequency ) <= getBandwidth()  )
				{
					return true;
				}
				//Requested channel is lower than the max locked frequency
				else if( channel.getMaxFrequency() <= maxLockedFrequency && 
					( maxLockedFrequency - channel.getMinFrequency() ) <= getBandwidth() )
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
							Recording recording, TunerChannel tunerChannel )
									throws RejectedExecutionException,
										   SourceException
	{
		//TODO: Write/replace with RecordingChannelSource
		
		TunerChannelSource source = null;
		
		if( canTuneChannel( tunerChannel ) )
		{
			mTunedChannels.add( tunerChannel );
			
			updateLOFrequency();
			
			//source = new TunerChannelSource( threadPoolManager, tuner, tunerChannel );
		}

		return source;
	}
	
	public void releaseChannel( TunerChannelSource tunerChannelSource )
	{
		//TODO: Write/replace with RecordingChannelSource
		if( tunerChannelSource != null )
		{
			mTunedChannels.remove( tunerChannelSource.getTunerChannel() );
		}
		else
		{
			Log.error( "Tuner Controller - couldn't find the tuned channel "
					+ "to release it" );
		}
	}
	
	/**
	 * Sets the Local Oscillator frequency to the middle of the currently
	 * locked frequency range, adjusting the left/right of a channel, if the
	 * middle falls within the locked range of any of the channels.  Note: this
	 * will fail to set the correct frequency if multiple overlapping channel
	 * bandwidths are locked in the exact middle of the total locked channel 
	 * frequency range.
	 *  
	 * @throws SourceException
	 */
	public void updateLOFrequency() throws SourceException
	{
		Collections.sort( mTunedChannels );

		long minLockedFrequency = mTunedChannels.get( 0 ).getMinFrequency();
		long maxLockedFrequency = mTunedChannels
				.get( mTunedChannels.size() - 1 ).getMaxFrequency();

		long middle = minLockedFrequency + 
				( ( maxLockedFrequency - minLockedFrequency ) / 2 );
		long middleMin = middle - 10000;
		long middleMax = middle + 10000;
		
		Iterator<TunerChannel> it = mTunedChannels.iterator();
		
		while( it.hasNext() )
		{
			TunerChannel lock = it.next();

			//If a locked channel overlaps our middle frequency lockout, adjust to
			//the left or the right of that channel, whichever is closer
			if( lock.getMinFrequency() < middleMax && middleMin < lock.getMaxFrequency() )
			{
				if( middleMax - lock.getMinFrequency() < lock.getMaxFrequency() - middleMin )
				{
					middle = lock.getMinFrequency() - 10000;
				}
				else
				{
					middle = lock.getMaxFrequency() + 10000;
				}
			}
		}
		
		mFrequencyController.setFrequency( middle );
	}

	/**
	 * Sets the listener to be notified any time that the tuner changes frequency
	 * or bandwidth/sample rate.
	 * 
	 * Note: this is normally used by the Tuner.  Any additional listeners can
	 * be registered on the tuner.
	 */
    public void addListener( FrequencyChangeListener listener )
    {
    	mFrequencyController.addListener( listener );
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( FrequencyChangeListener listener )
    {
    	mFrequencyController.removeListener( listener );
    }

	@Override
	public void setTunedFrequency(long frequency) throws SourceException {
		// TODO What does it mean to tune frequency on a recording?
	}

	@Override
	public int getCurrentSampleRate() throws SourceException {
		return sSAMPLE_RATE;
	}
}