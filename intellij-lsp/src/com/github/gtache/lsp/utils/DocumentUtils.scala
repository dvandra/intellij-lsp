package com.github.gtache.lsp.utils

import com.github.gtache.lsp.utils.ApplicationUtils.computableReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.util.TextRange
import com.intellij.util.DocumentUtil
import org.eclipse.lsp4j.Position

/**
  * Various methods to convert offsets / logical position / server position
  */
object DocumentUtils {

  private val LOG: Logger = Logger.getInstance(this.getClass)

  /**
    * Gets the line at the given offset given an editor and bolds the text between the given offsets
    *
    * @param editor      The editor
    * @param startOffset The starting offset
    * @param endOffset   The ending offset
    * @return The document line
    */
  def getLineText(editor: Editor, startOffset: Int, endOffset: Int): String = {
    computableReadAction(() => {
      val doc = editor.getDocument
      val lineIdx = doc.getLineNumber(startOffset)
      val lineStartOff = doc.getLineStartOffset(lineIdx)
      val lineEndOff = doc.getLineEndOffset(lineIdx)
      val line = doc.getText(new TextRange(lineStartOff, lineEndOff))
      val startOffsetInLine = startOffset - lineStartOff
      val endOffsetInLine = endOffset - lineStartOff
      line.substring(0, startOffsetInLine) + "<b>" + line.substring(startOffsetInLine, endOffsetInLine) + "</b>" + line.substring(endOffsetInLine)
    })
  }

  /**
    * Transforms a LogicalPosition (IntelliJ) to an LSP Position
    *
    * @param position the LogicalPosition
    * @param editor   The editor
    * @return the Position
    */
  def logicalToLSPPos(position: LogicalPosition, editor: Editor): Position = {
    computableReadAction(() => offsetToLSPPos(editor, editor.logicalPositionToOffset(position)))
  }

  /**
    * Calculates a Position given an editor and an offset
    *
    * @param editor The editor
    * @param offset The offset
    * @return an LSP position
    */
  def offsetToLSPPos(editor: Editor, offset: Int): Position = {
    computableReadAction(() => {
      val doc = editor.getDocument
      val line = doc.getLineNumber(offset)
      val lineStart = doc.getLineStartOffset(line)
      val lineTextBeforeOffset = doc.getText(TextRange.create(lineStart, offset))
      val column = lineTextBeforeOffset.length
      new Position(line, column)
    })
  }

  /**
    * Transforms an LSP position to an editor offset
    *
    * @param editor The editor
    * @param pos    The LSPPos
    * @return The offset
    */
  def LSPPosToOffset(editor: Editor, pos: Position): Int = {
    computableReadAction(() => {
      //TODO abort if basically wrong line?
      //TODO manage surrogates ?
      //TODO manage different sized-tabs
      val doc = editor.getDocument
      val line = math.max(0, math.min(pos.getLine, doc.getLineCount - 1))
      val docLength = doc.getTextLength
      val offset = doc.getLineStartOffset(line) + pos.getCharacter
      if (!DocumentUtil.isValidOffset(offset, doc)) {
        LOG.debug("Invalid offset : " + offset + ", doclength " + docLength)
      }
      math.min(math.max(offset, 0), docLength - 1)
    })
  }

  def expandOffsetToToken(editor: Editor, offset: Int): (Int, Int) = {
    computableReadAction(() => {
      val text = editor.getDocument.getText
      val negOffset = offset - text.take(offset).reverse.takeWhile(c => !c.isWhitespace).length
      val posOffset = offset + text.drop(offset).takeWhile(c => !c.isWhitespace).length
      (negOffset, posOffset)
    })
  }

}
