package org.example.clouddatastorage.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor // на подумать, возможно нужен так как Hibernate нуждается в POJO
@AllArgsConstructor
@Table(name = "file")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "format")
    private String format;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
