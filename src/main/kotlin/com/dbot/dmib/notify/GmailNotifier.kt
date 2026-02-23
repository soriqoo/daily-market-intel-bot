package com.dbot.dmib.notify

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class GmailNotifier(
    private val mailSender: JavaMailSender
) {
    fun send(to: String, subject: String, body: String) {
        val msg = SimpleMailMessage().apply {
            setTo(to)
            setSubject(subject)
            setText(body)
        }
        mailSender.send(msg)
    }
}
