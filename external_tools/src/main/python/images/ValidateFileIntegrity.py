"""
    Validate the integrity of image files

    In data-release 7 and 8 we had issues with image files that were corrupt.
    This was causing problems when Omero tried to upload them.

    This script checks the filetypes specified and reports any that seem 
    corrupt. It does this by attempting to load them using the imread function
    in matplotlib.pyplot
"""
import os
import sys
import argparse
import logging

# Import pyplot using 'agg' backend as there is no display on the server
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Run script to verify integrity of image files')
    parser.add_argument('-d', '--rootDir', dest='rootDir',
                        help='Root directory to start search for images')
    parser.add_argument('-t', '--filetypes', dest='filetypes',
                        default='jpg,jpeg,tif,tiff,png',
                        help='comma separated list of filetypes to verify')
    parser.add_argument('--logfile-path', dest='logfilePath', default=None,
                        help='path to save logfile')
    parser.add_argument('-f', '--filelist-path', dest='filelist_path',
                        help='path to file containing files to check')
    args = parser.parse_args()
    
    # Configure logger - if logging output file not specified create in this
    # directory with timestamp
    if args.logfilePath is None or args.logfilePath=="":
        import time
        import datetime
        t = time.time()
        tstamp = datetime.datetime.fromtimestamp(t).strftime('%Y%m%d_%H%M%S')
        logfile_path = "validate_file_integrity_" + tstamp + ".log"
    else:
        logfile_path = args.logfilePath

    log_format = '%(asctime)s - %(name)s - %(levelname)s:%(message)s'
    logging.basicConfig(format=log_format, filename=logfile_path,
                        level=logging.INFO)
    log_formatter = logging.Formatter(log_format)
    logger = logging.getLogger('ValidateFileIntegrity')
    root_logger = logging.getLogger()
    
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(log_formatter)
    root_logger.addHandler(console_handler)

    logger.info("running main method to validate integrity of the following image types: " + args.filetypes)
    
    # List of filetypes to check - including '.'
    filetypes = args.filetypes.split(',')
    for i in range(len(filetypes)):
        if filetypes[i][0] != '.':
            filetypes[i] = "."+filetypes[i]
            
    nfs_file_list = []
    # If --filelist-path is preset simply use this list
    if args.filelist_path is not None:
        logger.info('loading list of files to check from "' + args.filelist_path + '"')
        with open(args.filelist_path,'rt') as fid:
            for f in fid.readlines():
                f2 = f.strip('\n')
                ext = os.path.splitext(f2)[-1]
                try:
                    filetypes.index(ext)
                    nfs_file_list.append(f2)
                except ValueError:
                    continue
    elif args.rootDir is not None:
        logger.info('rootDir is "' + args.rootDir + '"')
        # Get the files in NFS
        file_tuple = os.walk(args.rootDir)
        nfs_file_list = []
        for ft in file_tuple:
            for f in ft[2]:
                ext = os.path.splitext(f)[-1]
                try:
                    filetypes.index(ext)
                    nfs_file_list.append(os.path.join(ft[0],f))
                except ValueError:
                    continue
    else:
        logger.error("At least one of --filelist-path or --rootDir must be supplied. Exiting")
        sys.exit(-1)

    logger.info("Number of files from NFS = " + str(len(nfs_file_list)))

    n_invalid = 0
    for f in nfs_file_list:
        try:
            im = plt.imread(f)
        except Exception as e:
            logger.error("Could not open " + f + ". Error was: " + str(e))
            n_invalid += 1

    logger.info("Number of invalid files: " + str(n_invalid))
