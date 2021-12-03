# File Layout using the MyCoRe SlotLayers
- Extension Name: MCRLayout
- Author: Tobias Lenhardt
- Minimum OCFL Version: 1.0
- OCFL Community Extensions Version: n/a
- Obsoletes: n/a
- Obsoleted by: n/a

## Notice:
> This is still under development and not ready for production!

## Overview

This storage root extension maps OCFL objects by segmenting their ID via the SlotLayout to bring back the original MyCoRe File Structure

## Configuration:

`MCR.IFS2.Store.\<Type>.SlotLayout`\
default: `4-2-2`

## Structure Sample:

```yaml
 mcrobject:DocPortal_document_00001122 
root
 - 0000
     - 11
         - ocfl object
             - vX
                 - content
                     - DocPortal_document_00001122.xml
```