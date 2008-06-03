document {
<attributes>
  <attribute name='cell-count' value='{fn:count(//ResultType[@name="Cell"]/CellInstance)}'/>
  <attribute name='cell-inner-area-mean' value='{fn:avg(//ResultType[@name="Cell"]/CellInstance/Result[@name="Cell: Inner Area"])}'/>
  <attribute name='cell-outer-area-mean' value='{fn:avg(//ResultType[@name="Cell"]/CellInstance/Result[@name="Cell: Outer Area"])}'/>
  <attribute name='image-1' value='{//Images/ImageFile[1]/text()}'/>
  <attribute name='image-2' value='{//Images/ImageFile[2]/text()}'/>
  <attribute name='image-3' value='{//Images/ImageFile[3]/text()}'/>
</attributes>
}
