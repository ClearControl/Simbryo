package simbryo.synthoscopy.microscope;

import java.util.concurrent.ConcurrentHashMap;

import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import simbryo.synthoscopy.microscope.parameters.ParameterInterface;
import simbryo.synthoscopy.phantom.PhantomRendererUtils;

/**
 * Microscope simulator base class
 *
 * 
 * @author royer
 */
public abstract class MicroscopeSimulatorBase implements
                                              MicroscopeSimulatorInterface
{

  protected ClearCLContext mContext;

  protected long[] mMainPhantomDimensions;

  protected ConcurrentHashMap<ParameterInterface<Void>, ClearCLImage> mPhantomMap =
                                                                                  new ConcurrentHashMap<>();

  protected ConcurrentHashMap<ParameterInterface<Number>, ConcurrentHashMap<Integer, Number>> mParametersMap =
                                                                                                             new ConcurrentHashMap<>();

  /**
   * Instanciates a microscope simulator.
   * 
   * @param pContext
   *          ClearCL context
   * 
   * @param pMainPhantomDimensions
   *          main phantom dimensions.
   */
  public MicroscopeSimulatorBase(ClearCLContext pContext,
                                 long... pMainPhantomDimensions)
  {
    mContext = pContext;
    mMainPhantomDimensions =
                           PhantomRendererUtils.adaptImageDimensionsToDevice(pContext.getDevice(),
                                                                             pMainPhantomDimensions);
  }

  /**
   * Returns the main phantom width
   * 
   * @return main phantom width
   */
  public long getWidth()
  {
    return mMainPhantomDimensions[0];
  }

  /**
   * Returns the main phantom height
   * 
   * @return main phantom height
   */
  public long getHeight()
  {
    return mMainPhantomDimensions[1];
  }

  /**
   * Returns the main phantom depth
   * 
   * @return main phantom depth
   */
  public long getDepth()
  {
    return mMainPhantomDimensions[2];
  }

  /**
   * Sets a given Phanton.
   * 
   * @param pParameter
   *          parameter
   * @param pPhantom
   *          phantom
   */
  @Override
  public void setPhantomParameter(ParameterInterface<Void> pParameter,
                                  ClearCLImage pPhantom)
  {
    mPhantomMap.put(pParameter, pPhantom);
  }

  /**
   * Returns a phantom image for a given parameter
   * 
   * @param pParameter
   *          phantom parameter name
   * @return phantom image
   */
  @Override
  public ClearCLImage getPhantomParameter(ParameterInterface<Void> pParameter)
  {
    return mPhantomMap.get(pParameter);
  }

  /**
   * Sets a parameter value for a given index
   * 
   * @param pParameter
   *          parameter name
   * @param pIndex
   *          index
   * @param pValue
   *          value
   */
  @Override
  public void setNumberParameter(ParameterInterface<Number> pParameter,
                                 int pIndex,
                                 Number pValue)
  {
    ConcurrentHashMap<Integer, Number> lConcurrentHashMap =
                                                          mParametersMap.get(pParameter);
    if (lConcurrentHashMap == null)
    {
      lConcurrentHashMap = new ConcurrentHashMap<Integer, Number>();
      mParametersMap.put(pParameter, lConcurrentHashMap);
    }

    lConcurrentHashMap.put(pIndex, pValue);
  }

  /**
   * Returns value for a given parameter and index
   * 
   * @param pParameter
   *          parameter name
   * @param pIndex
   *          index
   * @return value
   */
  @Override
  public Number getNumberParameter(ParameterInterface<Number> pParameter,
                                   int pIndex)
  {
    ConcurrentHashMap<Integer, Number> lConcurrentHashMap =
                                                          mParametersMap.get(pParameter);
    if (lConcurrentHashMap == null)
      return pParameter.getDefaultValue();

    Number lNumber = lConcurrentHashMap.get(pIndex);
    if (lNumber == null)
      return pParameter.getDefaultValue();
    return lNumber;
  }

  /**
   * Returns value for a given parameter and index. If the parameter is not
   * defined, a overriding default value is given which is used instead of the
   * parameter's default value.
   * 
   * @param pParameter
   *          parameter name
   * @param pIndex
   *          index
   * @param pDefaultOverideValue
   *          default overide value
   * @return value
   */
  @Override
  public Number getNumberParameter(ParameterInterface<Number> pParameter,
                                   int pIndex,
                                   Number pDefaultOverideValue)
  {
    ConcurrentHashMap<Integer, Number> lConcurrentHashMap =
                                                          mParametersMap.get(pParameter);
    if (lConcurrentHashMap == null)
      return pDefaultOverideValue;

    Number lNumber = lConcurrentHashMap.get(pIndex);

    if (lNumber == null)
      return pDefaultOverideValue;
    return lNumber;
  }

}
