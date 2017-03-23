package simbryo.synthoscopy.microscope.lightsheet.drosophila;

import java.io.IOException;

import clearcl.ClearCLContext;
import clearcl.viewer.ClearCLImageViewer;
import simbryo.dynamics.tissue.embryo.zoo.Drosophila;
import simbryo.synthoscopy.microscope.lightsheet.LightSheetMicroscopeSimulatorOrtho;
import simbryo.synthoscopy.microscope.parameters.PhantomParameter;
import simbryo.synthoscopy.phantom.fluo.impl.drosophila.DrosophilaHistoneFluorescence;
import simbryo.synthoscopy.phantom.scatter.impl.drosophila.DrosophilaScatteringPhantom;

/**
 * Light sheet microscope simulator with drosophila embryo
 *
 * @author royer
 */
public class LightSheetMicroscopeSimulatorDrosophila extends
                                                     LightSheetMicroscopeSimulatorOrtho
{

  private Drosophila mDrosophila;
  private DrosophilaHistoneFluorescence mDrosophilaFluorescencePhantom;
  private DrosophilaScatteringPhantom mDrosophilaScatteringPhantom;

  /**
   * Instanciates a light sheet microscope simulator with a simulated drosophila
   * embryo as sample
   * 
   * @param pContext
   *          ClearCL context to use
   * @param pNumberOfDetectionArms
   *          number of detection arms
   * @param pNumberOfIlluminationArms
   *          number of illumination arms
   * @param pMaxCameraResolution
   *          max width and height of camera images
   * @param pInitialDivisionTime
   *          initial 'division time' for embryo
   * @param pMainPhantomDimensions
   *          main phantom dimensions
   */
  public LightSheetMicroscopeSimulatorDrosophila(ClearCLContext pContext,
                                                 int pNumberOfDetectionArms,
                                                 int pNumberOfIlluminationArms,
                                                 int pMaxCameraResolution,
                                                 float pInitialDivisionTime,
                                                 long... pMainPhantomDimensions)
  {
    super(pContext,
          pNumberOfDetectionArms,
          pNumberOfIlluminationArms,
          pMaxCameraResolution,
          pMainPhantomDimensions);

    try
    {
      mDrosophila =
                  Drosophila.getDeveloppedEmbryo(pInitialDivisionTime);

      mDrosophilaFluorescencePhantom =
                                     new DrosophilaHistoneFluorescence(pContext,
                                                                       mDrosophila,
                                                                       getWidth(),
                                                                       getHeight(),
                                                                       getDepth());
      mDrosophilaFluorescencePhantom.render(true);

      mDrosophilaScatteringPhantom =
                                   new DrosophilaScatteringPhantom(pContext,
                                                                   mDrosophila,
                                                                   mDrosophilaFluorescencePhantom,
                                                                   getWidth() / 2,
                                                                   getHeight() / 2,
                                                                   getDepth() / 2);

      mDrosophilaScatteringPhantom.render(true);

      setPhantomParameter(PhantomParameter.Fluorescence,
                          mDrosophilaFluorescencePhantom.getImage());
      setPhantomParameter(PhantomParameter.Scattering,
                          mDrosophilaScatteringPhantom.getImage());
    }
    catch (IOException e)
    {
      e.printStackTrace();
      throw new RuntimeException("Problem while initializing phantoms",
                                 e);
    }

  }

  /**
   * Opens viewer for fluorescence phantom image.
   * 
   * @return viewer
   */
  public ClearCLImageViewer openViewerForFluorescencePhantom()
  {
    return mDrosophilaFluorescencePhantom.openViewer();
  }

  /**
   * Opens viewer for scattering phantom image.
   * 
   * @return viewer
   */
  public ClearCLImageViewer openViewerForScatteringPhantom()
  {
    return mDrosophilaScatteringPhantom.openViewer();
  }

  /**
   * Advance the drosophila embryo simulation by the given number and time
   * duration of steps.
   * 
   * @param pNumberOfSteps
   *          number of steps
   * @param pDeltaTime
   *          delta time for each step
   */
  public void simulationSteps(int pNumberOfSteps, float pDeltaTime)
  {
    mDrosophila.simulationSteps(pNumberOfSteps, pDeltaTime);
  }

  @Override
  public void render(boolean pWaitToFinish)
  {
    mDrosophilaFluorescencePhantom.render(false);
    mDrosophilaScatteringPhantom.render(false);
    super.render(pWaitToFinish);
  }

  @Override
  public void close() throws Exception
  {
    super.close();
    mDrosophilaScatteringPhantom.close();
    mDrosophilaFluorescencePhantom.close();
  }

}
