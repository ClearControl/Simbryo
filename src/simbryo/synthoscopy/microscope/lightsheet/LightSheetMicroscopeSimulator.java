package simbryo.synthoscopy.microscope.lightsheet;

import static java.lang.Math.min;
import static java.lang.Math.round;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLImage;
import clearcl.util.ElapsedTime;
import clearcl.viewer.ClearCLImageViewer;
import coremem.ContiguousMemoryInterface;
import simbryo.synthoscopy.camera.impl.SCMOSCameraRenderer;
import simbryo.synthoscopy.microscope.MicroscopeSimulatorBase;
import simbryo.synthoscopy.microscope.lightsheet.gui.jfx.LightSheetMicroscopeSimulatorViewer;
import simbryo.synthoscopy.microscope.parameters.CameraParameter;
import simbryo.synthoscopy.microscope.parameters.DetectionParameter;
import simbryo.synthoscopy.microscope.parameters.IlluminationParameter;
import simbryo.synthoscopy.microscope.parameters.PhantomParameter;
import simbryo.synthoscopy.microscope.parameters.StageParameter;
import simbryo.synthoscopy.microscope.parameters.UnitConversion;
import simbryo.synthoscopy.optics.detection.impl.widefield.WideFieldDetectionOptics;
import simbryo.synthoscopy.optics.illumination.impl.lightsheet.LightSheetIllumination;
import simbryo.util.geom.GeometryUtils;

/**
 * Light sheet microscope simulator
 *
 * @author royer
 */
public class LightSheetMicroscopeSimulator extends
                                           MicroscopeSimulatorBase
                                           implements
                                           LightSheetMicroscopeSimulatorInterface
{

  private static final float cDepthOfIlluminationInNormUnits = 1f;

  private static final int cLightMapScaleFactor = 4;

  private ArrayList<LightSheetIllumination> mLightSheetIlluminationList =
                                                                        new ArrayList<>();
  private ArrayList<WideFieldDetectionOptics> mWideFieldDetectionOpticsList =
                                                                            new ArrayList<>();
  private ArrayList<SCMOSCameraRenderer> mCameraRendererList =
                                                             new ArrayList<>();

  private ConcurrentHashMap<Integer, Matrix4f> mDetectionTransformationMatrixMap =
                                                                                 new ConcurrentHashMap<>();

  /**
   * Instanciates a light sheet microscope simulator given a ClearCL context
   * 
   * @param pContext
   *          ClearCL context
   * @param pMainPhantomDimensions
   *          main phantom dimensions.
   */
  public LightSheetMicroscopeSimulator(ClearCLContext pContext,
                                       long... pMainPhantomDimensions)
  {
    super(pContext, pMainPhantomDimensions);
  }

  @Override
  public LightSheetIllumination addLightSheet(Vector3f pAxisVector,
                                              Vector3f pNormalVector)
  {
    try
    {
      long lWidth = getWidth() / cLightMapScaleFactor;
      long lHeight = getHeight() / cLightMapScaleFactor;
      long lDepth =
                  min(getDepth(),
                      closestOddInteger(getDepth()
                                        * cDepthOfIlluminationInNormUnits));

      LightSheetIllumination lLightSheetIllumination =
                                                     new LightSheetIllumination(mContext,
                                                                                lWidth,
                                                                                lHeight,
                                                                                lDepth);

      lLightSheetIllumination.setLightSheetAxisVector(pAxisVector);
      lLightSheetIllumination.setLightSheetNormalVector(pNormalVector);

      mLightSheetIlluminationList.add(lLightSheetIllumination);
      return lLightSheetIllumination;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addDetectionPath(Matrix4f pDetectionTransformMatrix,
                               Vector3f pDownUpVector,
                               int pMaxCameraWidth,
                               int pMaxCameraHeight)
  {
    try
    {
      mDetectionTransformationMatrixMap.put(mWideFieldDetectionOpticsList.size(),
                                            pDetectionTransformMatrix);

      WideFieldDetectionOptics lWideFieldDetectionOptics =
                                                         new WideFieldDetectionOptics(mContext,
                                                                                      getWidth(),
                                                                                      getHeight());

      mWideFieldDetectionOpticsList.add(lWideFieldDetectionOptics);

      SCMOSCameraRenderer lSCMOSCameraRenderer =
                                               new SCMOSCameraRenderer(mContext,
                                                                       pMaxCameraWidth,
                                                                       pMaxCameraHeight);
      lSCMOSCameraRenderer.setDetectionDownUpVector(pDownUpVector);
      lWideFieldDetectionOptics.addUpdateListener(lSCMOSCameraRenderer);

      mCameraRendererList.add(lSCMOSCameraRenderer);
    }
    catch (IOException e)
    {
      throw new RuntimeException();
    }
  }

  /**
   * Must be called after all lightsheets and detection arms have been added.
   */
  @Override
  public void buildMicroscope()
  {
    for (LightSheetIllumination lLightSheetIllumination : mLightSheetIlluminationList)
      for (WideFieldDetectionOptics lWideFieldDetectionOptics : mWideFieldDetectionOpticsList)
      {
        lLightSheetIllumination.addUpdateListener(lWideFieldDetectionOptics);
      }
  }

  @Override
  public int getNumberOfLightSheets()
  {
    return mLightSheetIlluminationList.size();
  }

  @Override
  public int getNumberOfDetectionPaths()
  {
    return mWideFieldDetectionOpticsList.size();
  }

  @Override
  public LightSheetIllumination getLightSheet(int pIndex)
  {
    return mLightSheetIlluminationList.get(pIndex);
  }

  @Override
  public WideFieldDetectionOptics getDetectionOptics(int pIndex)
  {
    return mWideFieldDetectionOpticsList.get(pIndex);
  }

  /**
   * Returns camera for index
   * 
   * @param pIndex
   *          index
   * @return camera
   */
  @Override
  public SCMOSCameraRenderer getCameraRenderer(int pIndex)
  {
    return mCameraRendererList.get(pIndex);
  }

  private void applyParametersForLightSheet(int pLightSheetIndex,
                                            int pDetectionPathIndex)
  {
    float lLengthConversionfactor =
                                  getNumberParameter(UnitConversion.Length,
                                                     0).floatValue();
    float lLaserPowerConversionfactor =
                                      getNumberParameter(UnitConversion.LaserIntensity,
                                                         0).floatValue();

    LightSheetIllumination lLightSheetIllumination =
                                                   mLightSheetIlluminationList.get(pLightSheetIndex);

    lLightSheetIllumination.setDetectionTransformMatrix(getDetectionTransformMatrix(pDetectionPathIndex));
    lLightSheetIllumination.setPhantomTransformMatrix(getStageTransformMatrix());

    lLightSheetIllumination.setScatteringPhantom(getPhantomParameter(PhantomParameter.Scattering));

    float lIntensity =
                     getNumberParameter(IlluminationParameter.Intensity,
                                        pLightSheetIndex).floatValue()
                       / lLaserPowerConversionfactor;

    float lWaveLength =
                      getNumberParameter(IlluminationParameter.Wavelength,
                                         pLightSheetIndex).floatValue();

    float xl = (getNumberParameter(IlluminationParameter.X,
                                   pLightSheetIndex).floatValue()
                / lLengthConversionfactor)
               + 0.5f;
    float yl = (getNumberParameter(IlluminationParameter.Y,
                                   pLightSheetIndex).floatValue()
                / lLengthConversionfactor)
               + 0.5f;
    float zl = (getNumberParameter(IlluminationParameter.Z,
                                   pLightSheetIndex).floatValue()
                / lLengthConversionfactor)
               + 0.5f;

    float height = getNumberParameter(IlluminationParameter.Height,
                                      pLightSheetIndex).floatValue()
                   / lLengthConversionfactor;

    float alpha = getNumberParameter(IlluminationParameter.Alpha,
                                     pLightSheetIndex).floatValue();
    float beta = getNumberParameter(IlluminationParameter.Beta,
                                    pLightSheetIndex).floatValue();
    float gamma = getNumberParameter(IlluminationParameter.Gamma,
                                     pLightSheetIndex).floatValue();
    float theta = getNumberParameter(IlluminationParameter.Theta,
                                     pLightSheetIndex).floatValue();

    lLightSheetIllumination.setIntensity(lIntensity);
    lLightSheetIllumination.setLightWavelength(lWaveLength);
    lLightSheetIllumination.setLightSheetPosition(xl, yl, zl);
    lLightSheetIllumination.setLightSheetHeigth(height);
    lLightSheetIllumination.setOrientationWithAnglesInDegrees(alpha,
                                                              beta,
                                                              gamma);
    lLightSheetIllumination.setLightSheetThetaInDeg(theta);

  }

  private void applyParametersForDetectionPath(int pDetectionPathIndex,
                                               ClearCLImage pLightMapImage)
  {
    float lLengthConversionfactor =
                                  getNumberParameter(UnitConversion.Length,
                                                     0).floatValue();

    WideFieldDetectionOptics lWideFieldDetectionOptics =
                                                       mWideFieldDetectionOpticsList.get(pDetectionPathIndex);
    SCMOSCameraRenderer lSCMOSCameraRenderer =
                                             mCameraRendererList.get(pDetectionPathIndex);

    ClearCLImage lFluorescencePhantomImage =
                                           getPhantomParameter(PhantomParameter.Fluorescence);
    ClearCLImage lScatteringPhantomImage =
                                         getPhantomParameter(PhantomParameter.Scattering);

    float lIntensity =
                     getNumberParameter(DetectionParameter.Intensity,
                                        pDetectionPathIndex).floatValue();

    float lWaveLength =
                      getNumberParameter(DetectionParameter.Wavelength,
                                         pDetectionPathIndex).floatValue();

    float lFocusZ = (getNumberParameter(DetectionParameter.FocusZ,
                                        pDetectionPathIndex).floatValue()
                     / lLengthConversionfactor)
                    + 0.5f;

    long lDetectionImageWidth = lFluorescencePhantomImage.getWidth();
    long lDetectionImageHeight =
                               lFluorescencePhantomImage.getHeight();

    int lROIOffsetX =
                    getNumberParameter(CameraParameter.ROIOffsetX,
                                       pDetectionPathIndex).intValue();
    int lROIOffsetY =
                    getNumberParameter(CameraParameter.ROIOffsetY,
                                       pDetectionPathIndex).intValue();

    int lROIWidth =
                  getNumberParameter(CameraParameter.ROIWidth,
                                     pDetectionPathIndex,
                                     lSCMOSCameraRenderer.getMaxWidth()).intValue();
    int lROIHeight =
                   getNumberParameter(CameraParameter.ROIHeight,
                                      pDetectionPathIndex,
                                      lSCMOSCameraRenderer.getMaxHeight()).intValue();

    lWideFieldDetectionOptics.setFluorescencePhantomImage(lFluorescencePhantomImage);
    lWideFieldDetectionOptics.setScatteringPhantomImage(lScatteringPhantomImage);
    lWideFieldDetectionOptics.setLightMapImage(pLightMapImage);

    lWideFieldDetectionOptics.setIntensity(lIntensity);
    lWideFieldDetectionOptics.setLightWavelength(lWaveLength);
    lWideFieldDetectionOptics.setZFocusPosition(lFocusZ);
    lWideFieldDetectionOptics.setWidth(lDetectionImageWidth);
    lWideFieldDetectionOptics.setHeight(lDetectionImageHeight);

    lWideFieldDetectionOptics.setPhantomTransformMatrix(getStageAndDetectionTransformMatrix(pDetectionPathIndex));

    lSCMOSCameraRenderer.setDetectionImage(lWideFieldDetectionOptics.getImage());

    lSCMOSCameraRenderer.setCenteredROI(lROIOffsetX,
                                        lROIOffsetY,
                                        lROIWidth,
                                        lROIHeight);

  }

  private Matrix4f getStageAndDetectionTransformMatrix(int pDetectionPathIndex)
  {
    Matrix4f lCombinedTransformMatrix =
                                      GeometryUtils.multiply(getStageTransformMatrix(),
                                                             getDetectionTransformMatrix(pDetectionPathIndex));
    return lCombinedTransformMatrix;
  }

  private Matrix4f getDetectionTransformMatrix(int pDetectionPathIndex)
  {
    Matrix4f lDetectionTransformationMatrix =
                                            mDetectionTransformationMatrixMap.get(pDetectionPathIndex);
    return new Matrix4f(lDetectionTransformationMatrix);
  }

  private Matrix4f getStageTransformMatrix()
  {
    float lLengthConversionfactor =
                                  getNumberParameter(UnitConversion.Length,
                                                     0).floatValue();

    float lStageX = getNumberParameter(StageParameter.StageX, 0, 0)
                                                                   .floatValue()
                    / lLengthConversionfactor;
    float lStageY = getNumberParameter(StageParameter.StageY, 0, 0)
                                                                   .floatValue()
                    / lLengthConversionfactor;
    float lStageZ = getNumberParameter(StageParameter.StageZ, 0, 0)
                                                                   .floatValue()
                    / lLengthConversionfactor;

    float lStageRX = getNumberParameter(StageParameter.StageRX,
                                        0,
                                        0).floatValue();

    float lStageRY = getNumberParameter(StageParameter.StageRY,
                                        0,
                                        0).floatValue();
    float lStageRZ = getNumberParameter(StageParameter.StageRZ,
                                        0,
                                        0).floatValue();

    Vector3f lCenter = new Vector3f(0.5f, 0.5f, 0.5f);

    Matrix4f lMatrixRX =
                       GeometryUtils.rotX((float) Math.toRadians(lStageRX),
                                          lCenter);
    Matrix4f lMatrixRY =
                       GeometryUtils.rotY((float) Math.toRadians(lStageRY),
                                          lCenter);
    Matrix4f lMatrixRZ =
                       GeometryUtils.rotZ((float) Math.toRadians(lStageRZ),
                                          lCenter);

    Matrix4f lMatrix = GeometryUtils.multiply(lMatrixRX,
                                              lMatrixRY,
                                              lMatrixRZ);

    GeometryUtils.addTranslation(lMatrix, lStageX, lStageY, lStageZ);

    return lMatrix;
  }

  @Override
  public void render(boolean pWaitToFinish)
  {
    int lNumberOfDetectionPath = mWideFieldDetectionOpticsList.size();

    for (int d = 0; d < lNumberOfDetectionPath; d++)
    {
      render(d, pWaitToFinish && (d == lNumberOfDetectionPath - 1));
    }
  }

  @Override
  public void render(int pDetectionIndex, boolean pWaitToFinish)
  {
    WideFieldDetectionOptics lWideFieldDetectionOptics =
                                                       mWideFieldDetectionOpticsList.get(pDetectionIndex);
    SCMOSCameraRenderer lSCMOSCameraRenderer =
                                             mCameraRendererList.get(pDetectionIndex);

    ClearCLImage lCurrentLightMap = null;
    int lNumberOfLightSheets = mLightSheetIlluminationList.size();
    for (int lLightSheetIndex =
                              0; lLightSheetIndex < lNumberOfLightSheets; lLightSheetIndex++)
    {
      LightSheetIllumination lLightSheetIllumination =
                                                     mLightSheetIlluminationList.get(lLightSheetIndex);
      applyParametersForLightSheet(lLightSheetIndex, pDetectionIndex);
      lLightSheetIllumination.setInputImage(lCurrentLightMap);
      ElapsedTime.measure("renderlightsheet",
                          () -> lLightSheetIllumination.render(false));

      lCurrentLightMap = lLightSheetIllumination.getImage();

    }

    applyParametersForDetectionPath(pDetectionIndex,
                                    lCurrentLightMap);

    ElapsedTime.measure("renderdetection",
                        () -> lWideFieldDetectionOptics.render(false));

    ElapsedTime.measure("rendercameraimage",
                        () -> lSCMOSCameraRenderer.render(pWaitToFinish));/**/

    lWideFieldDetectionOptics.clearUpdate();
    lSCMOSCameraRenderer.clearUpdate();
  }

  @Override
  public ClearCLImage getCameraImage(int pIndex)
  {
    return mCameraRendererList.get(pIndex).getImage();
  }

  @Override
  public ClearCLImageViewer openViewerForCameraImage(int pIndex)
  {
    SCMOSCameraRenderer lSCMOSCameraRenderer =
                                             mCameraRendererList.get(pIndex);

    final ClearCLImageViewer lViewImage =
                                        lSCMOSCameraRenderer.openViewer();
    return lViewImage;
  }

  @Override
  public ClearCLImageViewer openViewerForLightMap(int pIndex)
  {
    return getLightSheet(pIndex).openViewer();
  }

  @Override
  public LightSheetMicroscopeSimulatorViewer openViewerForControls()
  {
    LightSheetMicroscopeSimulatorViewer lViewer =
                                                new LightSheetMicroscopeSimulatorViewer(this,
                                                                                        "LightSheetSimulator");

    return lViewer;
  }

  @Override
  public void copyTo(int pCameraIndex,
                     ContiguousMemoryInterface pContiguousMemory,
                     long pOffsetInContiguousMemory,
                     boolean pBlocking)
  {
    ClearCLImage lCameraImage = getCameraImage(pCameraIndex);
    ContiguousMemoryInterface lImagePlane =
                                          pContiguousMemory.subRegion(pOffsetInContiguousMemory,
                                                                      lCameraImage.getSizeInBytes());
    lCameraImage.writeTo(lImagePlane, pBlocking);
  }

  @Override
  public ClearCLBuffer getCameraImageBuffer(int pCameraIndex)
  {
    return mCameraRendererList.get(pCameraIndex)
                              .getCameraImageBuffer();
  }

  private int closestOddInteger(float pValue)
  {
    return round((pValue - 1) / 2) * 2 + 1;
  }

  @Override
  public void close() throws Exception
  {
    for (LightSheetIllumination lLightSheetIllumination : mLightSheetIlluminationList)
    {
      lLightSheetIllumination.close();
    }

    for (WideFieldDetectionOptics lWideFieldDetectionOptics : mWideFieldDetectionOpticsList)
    {
      lWideFieldDetectionOptics.close();
    }

    for (SCMOSCameraRenderer lScmosCameraRenderer : mCameraRendererList)
    {
      lScmosCameraRenderer.close();
    }

  }

}
