package org.mousephenotype.cda.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Observation parameter constants.
 *
 * Created by mrelac on 02/07/2015.
 */
public class Constants {

    public static final List<String> viabilityParameters = Arrays.asList(
            "IMPC_VIA_001_001", "IMPC_VIA_002_001", "IMPC_EVL_001_001",
            "IMPC_EVM_001_001", "IMPC_EVP_001_001", "IMPC_EVO_001_001");

    // 03-Aug-2017 (mrelac) Do not include IMPC_VIA_002_001 in this list unless you want duplicate genes that qualify for both
    // IMPC_VIA_001_001 and IMPC_VIA_002_001 (see gene_symbol App or Ctsd, for example)
    public static final List<String> adultViabilityParameters = Arrays.asList(
            "IMPC_VIA_001_001");


    public static final List<String> weightParameters = Arrays.asList(
            "'IMPC_GRS_003_001'", "'IMPC_CAL_001_001'", "'IMPC_DXA_001_001'",
            "'IMPC_HWT_007_001'", "'IMPC_PAT_049_001'", "'IMPC_BWT_001_001'",
            "'IMPC_ABR_001_001'", "'IMPC_CHL_001_001'", "'TCP_CHL_001_001'",
            "'HMGU_ROT_004_001'", "'ESLIM_001_001_001'", "'ESLIM_002_001_001'",
            "'ESLIM_003_001_001'", "'ESLIM_004_001_001'", "'ESLIM_005_001_001'",
            "'ESLIM_020_001_001'", "'ESLIM_022_001_001'", "'ESLIM_009_001_003'",
            "'ESLIM_010_001_003'", "'ESLIM_011_001_011'", "'ESLIM_012_001_005'",
            "'ESLIM_013_001_018'", "'ESLIM_022_001_001'", "'GMC_916_001_022'",
            "'GMC_908_001_001'", "'GMC_900_001_001'", "'GMC_926_001_003'",
            "'GMC_922_001_002'", "'GMC_923_001_001'", "'GMC_921_001_002'",
            "'GMC_902_001_003'", "'GMC_912_001_018'", "'GMC_917_001_001'",
            "'GMC_920_001_001'", "'GMC_909_001_002'", "'GMC_914_001_001'",
            "'ICS_HOT_002_001'", "'ICS_ROT_004_001'", "'IMPC_OWT_001_001'",
            "'IMPC_PAT_049_002'", "'TCP_CHL_043_001'");
}
