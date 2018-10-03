/***********************************************************************
** Purpose:    This is the test version of the select performed in the
**             QueryManager.java
** Comments:   Use for testing.
*/
SELECT v_custhandle
     , v_sitehandle
     , v_servicehandle
     , v_ordhandle
     , v_orderstatus
     , v_ordertype
     , v_ctrhandle
     , v_sbahandle
     , v_enduserhandle
     , v_exchangerate
     , v_createdate
     , vse_versionserviceelementid
     , vse_lupddate
     , vses_statustypecode
     , se_serviceelementid
     , se_usid
     , se_serviceelementclass
     , se_serviceelementname
     , se_description
     , se_creationdate
     , se_grandfatherdate
     , se_startbillingdate
     , se_endbillingdate
     , se_prodid
     , u_systemusername
     , u_systemcode
     , i_lupddate
     , i_changetypecode
     , t_serviceattributeid
     , t_serviceattributename
     , t_value
     , vse_createdate
  FROM viw_version_element_attribute v
 WHERE v_custhandle = 1111
   AND v_sitehandle = 'Test Site'
   AND v_servicehandle = 'FRAME_RELAY_TEST'
   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid
                                  FROM viw_version_element_status vv
                                 WHERE v_custhandle = 1111
                                   AND v_sitehandle = 'Test Site'
                                   AND v_servicehandle = 'FRAME_RELAY_TEST'
                                   AND vses_statustypecode IN ('Delete', 'Disconnect')
                                   AND vv.se_serviceelementid =  v.se_serviceelementid
                                   AND vv.vse_lupddate >= v.vse_lupddate)
 ORDER BY se_serviceelementid, vse_lupddate DESC, i_lupddate DESC

/***********************************************************************
** Purpose:    This is the same select as above, ready to be pasted into
**             QueryManager.java. Test values replaced with ? marks.
** Comments:   Use for testing.

  "SELECT v_custhandle                                                                   " // 1
+ "     , v_sitehandle                                                                   " // 2
+ "     , v_servicehandle                                                                " // 3
+ "     , v_ordhandle                                                                    " // 4
+ "     , v_orderstatus                                                                  " // 5
+ "     , v_ordertype                                                                    " // 6
+ "     , v_ctrhandle                                                                    " // 7
+ "     , v_sbahandle                                                                    " // 8
+ "     , v_enduserhandle                                                                " // 9
+ "     , v_exchangerate                                                                 " // 10
+ "     , v_createdate                                                                   " // 11
+ "     , vse_versionserviceelementid                                                    " // 12
+ "     , vse_lupddate                                                                   " // 13
+ "     , vses_statustypecode                                                            " // 14
+ "     , se_serviceelementid                                                            " // 15
+ "     , se_usid                                                                        " // 16
+ "     , se_serviceelementclass                                                         " // 17
+ "     , se_serviceelementname                                                          " // 18
+ "     , se_description                                                                 " // 19
+ "     , se_creationdate                                                                " // 20
+ "     , se_grandfatherdate                                                             " // 21
+ "     , se_startbillingdate                                                            " // 22
+ "     , se_endbillingdate                                                              " // 23
+ "     , se_prodid                                                                      " // 24
+ "     , u_systemusername                                                               " // 25
+ "     , u_systemcode                                                                   " // 26
+ "     , i_lupddate                                                                     " // 27
+ "     , i_changetypecode                                                               " // 28
+ "     , t_serviceattributeid                                                           " // 29
+ "     , t_serviceattributename                                                         " // 30
+ "     , t_value                                                                        " // 31
+ "     , vse_createdate                                                                 " // 32
+ "  FROM viw_version_element_attribute v                                                "
+ " WHERE v_custhandle = ?                                                               "
+ "   AND v_sitehandle = ?                                                               "
+ "   AND v_servicehandle = ?                                                            "
+ "   AND se_serviceelementid NOT IN (SELECT DISTINCT se_serviceelementid                "
+ "                                  FROM viw_version_element_status vv                  "
+ "                                 WHERE v_custhandle = ?                               "
+ "                                   AND v_sitehandle = ?                               "
+ "                                   AND v_servicehandle = ?                            "
+ "                                   AND vses_statustypecode IN ('Delete', 'Disconnect')"
+ "                                   AND vv.se_serviceelementid =  v.se_serviceelementid"
+ "                                   AND vv.vse_lupddate >= v.vse_lupddate)             "
+ " ORDER BY se_serviceelementid, vse_lupddate DESC, i_lupddate DESC                     "

*/
