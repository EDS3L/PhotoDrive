package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.vo.MailMessage;

public interface MailSender {
    void send(MailMessage message);
}