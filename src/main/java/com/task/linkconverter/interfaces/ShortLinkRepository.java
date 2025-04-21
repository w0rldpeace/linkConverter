package com.task.linkconverter.interfaces;

import com.task.linkconverter.model.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByOriginalUrl(String originalUrl);
    Optional<ShortLink> findByShortLink(String shortLink);
}
