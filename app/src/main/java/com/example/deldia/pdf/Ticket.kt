package com.example.deldia.pdf

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.deldia.models.Operation
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat


class Ticket(var context: Context, var operation: Operation) {

    private val lato = FontFactory.getFont("lato", "utf-8",false, 8f)
    private val abel = FontFactory.getFont("abel", "utf-8",false, 8f)
    private val ticketing = BaseFont.createFont("res/font/ticketing_regular.ttf", BaseFont.IDENTITY_H, false, true)
    private val squareBold = BaseFont.createFont("res/font/square_bold.ttf", BaseFont.IDENTITY_H, true, true)
    private val squareCondensed = BaseFont.createFont("res/font/square_condensed.ttf", BaseFont.IDENTITY_H, false, true)

    private val fontCourier8 = FontFactory.getFont(FontFactory.COURIER, 8f, BaseColor.BLACK)
    private val fontCourier10 = FontFactory.getFont(FontFactory.COURIER, 10f, BaseColor.BLACK)
    private val fontCourierBold10 = FontFactory.getFont(FontFactory.COURIER, 10f, Font.BOLD, BaseColor.BLACK)
    private val fontCourierBold12 = FontFactory.getFont(FontFactory.COURIER, 12f, Font.BOLD, BaseColor.BLACK)
    private val fontHelvetica12 = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
    private val fontHelveticaBold8 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, BaseColor.BLACK)
    private val fontHelvetica8 = FontFactory.getFont(FontFactory.HELVETICA, 8f, BaseColor.BLACK)
    private val fontTimes12 = FontFactory.getFont(FontFactory.TIMES, 12f, BaseColor.BLACK)
    private val fontArialBold12 = FontFactory.getFont("arial", 12f, Font.BOLD, BaseColor.BLACK)

    private val fontTicketing12 = Font(ticketing, 12f)
    private val fontTicketing8 = Font(ticketing, 8f)
    private val fontTicketing6 = Font(ticketing, 6f)
    private val fontSquareBold12 = Font(squareBold, 12f)
    private val fontSquareBold14 = Font(squareBold, 14f)
    private val fontSquareBold8 = Font(squareBold, 8f)
    private val fontSquareCondensed8 = Font(squareCondensed, 8f)
    private val fontSquareCondensed10 = Font(squareCondensed, 10f)
    private val fontSquareCondensed12 = Font(squareCondensed, 12f)
    private val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    private val dfZero: DecimalFormat = DecimalFormat("0.00")

    fun getFilePathDirectory(): String {
        val dir = File(path, "4SOFT-PDF")
        return File(dir, "${operation.documentNumber}.pdf").absolutePath
    }

    fun generatePDF(){

//        var path = Environment.getExternalStorageDirectory().absolutePath + "/4SOFT-PDF"

//        val storeDirectory = Environment.getExternalStorageDirectory() // DCIM folder

        val dir = File(path, "4SOFT-PDF")
        if (!dir.exists())
            dir.mkdirs()
        if (dir.exists() and dir.canRead()) {
            val file = File(dir, "${operation.documentNumber}.pdf")
            val fileOutputStream = FileOutputStream(file)
            val inch = 72f // 1 inch corresponds with 72 user units
            val document = Document()
            document.pageSize = Rectangle(2.28346f * inch,11f * inch)
            document.setMargins(0.005f * inch,0.005f * inch, 0.005f * inch, 0.005f * inch)
            PdfWriter.getInstance(document, fileOutputStream)
            document.open()


            val title = Paragraph("DELDIA", fontSquareBold12)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)

            val title2 = Paragraph("DISTRIBUCIONES SAC", fontSquareBold14)
            title2.leading = 12f
            title2.alignment = Element.ALIGN_CENTER
            document.add(title2)

            val title3 = Paragraph("DISTRIBUIDOR FRITO LAY-KARINTO", fontSquareCondensed10)
            title3.leading = 10f
            title3.alignment = Element.ALIGN_CENTER
            document.add(title3)

            val title4 = Paragraph("Guillermo Delgado Miranda", fontSquareCondensed10)
            title4.leading = 10f
            title4.alignment = Element.ALIGN_CENTER
            document.add(title4)

            val title5 = Paragraph("TELEFONO 958245315", fontSquareCondensed10)
            title5.leading = 10f
            title5.alignment = Element.ALIGN_CENTER
            document.add(title5)

            val voidLabel = Paragraph("---------------------------",fontCourier10 )
            voidLabel.leading = 8f
            voidLabel.alignment = Element.ALIGN_CENTER
            document.add(voidLabel)

            val title6 = Paragraph(operation.documentTypeDisplay, fontSquareBold12)
            title6.leading = 8f
            title6.alignment = Element.ALIGN_CENTER
            document.add(title6)

            val title7 = Paragraph(operation.documentNumber, fontSquareBold12)
            title7.leading = 10f
            title7.alignment = Element.ALIGN_CENTER
            document.add(title7)

            document.add(voidLabel)

            val clientLabel = Paragraph("CLIENTE: " + operation.clientFullName, fontCourier8)
            clientLabel.leading = 10f
            clientLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(clientLabel)

            val clientDocumentLabel = Paragraph(operation.clientDocumentType + ": " + operation.clientDocumentNumber, fontCourier8)
            clientDocumentLabel.leading = 10f
            clientDocumentLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(clientDocumentLabel)

            val clientAddressLabel = Paragraph("DIRECCION: " + operation.clientAddress, fontCourier8)
            clientAddressLabel.leading = 10f
            clientAddressLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(clientAddressLabel)

            val clientCellphoneLabel = Paragraph("CELULAR: " + operation.clientCellphone, fontCourier8)
            clientCellphoneLabel.leading = 10f
            clientCellphoneLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(clientCellphoneLabel)

            val dateLabel = Paragraph("FECHA: " + operation.operationDate, fontCourier8)
            dateLabel.leading = 10f
            dateLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(dateLabel)

            val timeLabel = Paragraph("HORA: " + operation.operationTime, fontCourier8)
            timeLabel.leading = 10f
            timeLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(timeLabel)

            val userLabel = Paragraph("VENDEDOR: " + operation.userFullName, fontCourier8)
            userLabel.leading = 10f
            userLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(userLabel)

            val gangLabel = Paragraph("CUADRILLA: ${operation.gangName} ${operation.clientVisitDayDisplay.subSequence(0,2)}-${operation.clientObservation}", fontCourier8)
            gangLabel.leading = 10f
            gangLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(gangLabel)

            val physicalDistributionLabel = Paragraph("TIPO ENTREGA: ${operation.physicalDistributionDisplay}", fontCourier8)
            physicalDistributionLabel.leading = 10f
            physicalDistributionLabel.alignment = Element.ALIGN_JUSTIFIED
            document.add(physicalDistributionLabel)

            document.add(voidLabel)
//            document.add(Chunk.NEWLINE)

            val table = PdfPTable(1)
            table.widthPercentage = 100f
            table.setWidths( floatArrayOf(100f))

//            for(headerTitle in arrayOf("DESCRIPCION", "PVP", "CANT.", "IMP.")){
//                val header = PdfPCell()
////                header.backgroundColor = BaseColor.WHITE
//                header.horizontalAlignment = Element.ALIGN_CENTER
//                header.borderWidth = 0f
//                header.paddingTop = 4f
//                header.phrase = Phrase(headerTitle, fontTicketing8)
//                table.addCell(header)
//            }

            for (item in operation.details){
                val productCell = PdfPCell(Phrase(item.productSaleName.trim(), fontCourier8))
                productCell.borderWidth = 0f
                productCell.paddingTop = 0f
                productCell.paddingBottom = 0f
                productCell.verticalAlignment = Element.ALIGN_MIDDLE
                productCell.horizontalAlignment = Element.ALIGN_JUSTIFIED
                table.addCell(productCell)

                val amountCell = PdfPCell(Phrase("${dfZero.format(item.price)}  x  ${item.quantity}  =  ${dfZero.format(item.subtotal)}", fontCourier8))
                amountCell.borderWidth = 0f
                productCell.paddingTop = 0f
                productCell.paddingBottom = 0f
                amountCell.verticalAlignment = Element.ALIGN_MIDDLE
                amountCell.horizontalAlignment = Element.ALIGN_RIGHT
                table.addCell(amountCell)

            }
            document.add(table)
            document.add(voidLabel)

            val table2 = PdfPTable(2)
            table2.widthPercentage = 100f
            table2.setWidths( floatArrayOf(70f, 30f))

            val base: Double = Math.round(operation.total / (1.18) * 100.0) / 100.0
            val igv: Double = Math.round((operation.total - base) * 100.0) / 100.0

            val baseLabelCell = PdfPCell(Phrase("OP. GRAVADAS:", fontCourier8))
            baseLabelCell.horizontalAlignment = Element.ALIGN_RIGHT
            baseLabelCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(baseLabelCell)

            val baseValueCell = PdfPCell(Phrase("S/ ${dfZero.format(base)}", fontCourier8))
            baseValueCell.verticalAlignment = Element.ALIGN_MIDDLE
            baseValueCell.horizontalAlignment = Element.ALIGN_RIGHT
            baseValueCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(baseValueCell)

            val igvLabelCell = PdfPCell(Phrase("IGV:", fontCourier8))
            igvLabelCell.horizontalAlignment = Element.ALIGN_RIGHT
            igvLabelCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(igvLabelCell)

            val igvValueCell = PdfPCell(Phrase("S/ ${dfZero.format(igv)}", fontCourier8))
            igvValueCell.verticalAlignment = Element.ALIGN_MIDDLE
            igvValueCell.horizontalAlignment = Element.ALIGN_RIGHT
            igvValueCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(igvValueCell)

            val totalLabelCell = PdfPCell(Phrase("TOTAL A PAGAR:", fontCourier8))
            totalLabelCell.horizontalAlignment = Element.ALIGN_RIGHT
            totalLabelCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(totalLabelCell)

            val totalValueCell = PdfPCell(Phrase("S/ ${dfZero.format(operation.total)}", fontCourier8))
            totalValueCell.verticalAlignment = Element.ALIGN_MIDDLE
            totalValueCell.horizontalAlignment = Element.ALIGN_RIGHT
            totalValueCell.borderWidth = 0f
            baseLabelCell.paddingTop = 0f
            baseLabelCell.paddingBottom = 0f
            table2.addCell(totalValueCell)

            val numberToLetterCell = PdfPCell(Phrase("\nSON: ${operation.numberToCurrency} \n\n", fontSquareCondensed10))
            numberToLetterCell.borderWidth = 0f
            numberToLetterCell.paddingTop = 5f
            numberToLetterCell.colspan = 2
            numberToLetterCell.verticalAlignment = Element.ALIGN_MIDDLE
            numberToLetterCell.horizontalAlignment = Element.ALIGN_JUSTIFIED
            table2.addCell(numberToLetterCell)

            document.add(table2)

            val footer = Paragraph("Representacion impresa de la \n", fontCourier8)
            val footer1 = Paragraph("${operation.documentTypeDisplay} DE VENTA ELECTRONICA,\n", fontCourier8)
            val footer2 = Paragraph("consulte el documento en \n", fontCourier8)
            val footer3 = Paragraph("https://4soluciones.pse.pe/20600854535\n", fontCourier8)
            val footer4 = Paragraph("Emitido mediante un PROVEEDOR\n", fontCourier8)
            val footer5 = Paragraph("Autorizado por la SUNAT mediante Resolucion de Intendencia\n", fontCourier8)
            val footer6 = Paragraph("Nro.304 - 005 - 0005315 \n", fontCourier8)
            val footer7 = Paragraph("¡GRACIAS POR SU COMPRA!", fontCourier8)
            footer.alignment = Element.ALIGN_CENTER
            footer.leading = 10f
            footer1.alignment = Element.ALIGN_CENTER
            footer2.alignment = Element.ALIGN_CENTER
            footer3.alignment = Element.ALIGN_CENTER
            footer4.alignment = Element.ALIGN_CENTER
            footer5.alignment = Element.ALIGN_CENTER
            footer6.alignment = Element.ALIGN_CENTER
            footer7.alignment = Element.ALIGN_CENTER
            document.add(footer)
            document.add(footer1)
            document.add(footer2)
            document.add(footer3)
            document.add(footer4)
            document.add(footer5)
            document.add(footer6)
            document.add(footer7)
            document.close()
        }else{
            Log.d("MIKE", "no puede crear directorio...")
        }

    }

    fun loadPDF(){
//        val intent = Intent(context as Activity, ViewActivity::class.java)
//        startActivity(intent!!, Bundle)
//        getContext().startActivity(Intent(getContext(), ViewActivity::class.java))
    }
}